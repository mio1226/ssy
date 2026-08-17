package com.harddisk.module.feishu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.harddisk.module.rule.entity.RuleConfig;
import com.harddisk.module.rule.mapper.RuleConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RuleConfigMapper ruleConfigMapper;

    private static final String BASE_URL = "https://open.feishu.cn/open-apis";
    private static final long TOKEN_EXPIRE_BUFFER = 60;
    private static final long CACHE_TTL_MS = 30_000;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    private final Map<String, ConfigCacheEntry> configCache = new ConcurrentHashMap<>();

    private static class ConfigCacheEntry {
        final String value;
        final long expireAt;
        ConfigCacheEntry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
        boolean isValid() {
            return System.currentTimeMillis() < expireAt;
        }
    }

    private String getConfigValue(String key) {
        ConfigCacheEntry entry = configCache.get(key);
        if (entry != null && entry.isValid()) {
            return entry.value;
        }
        RuleConfig config = ruleConfigMapper.selectOne(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getRuleKey, key)
                        .eq(RuleConfig::getStatus, 1));
        String value = config != null ? config.getRuleValue() : "";
        configCache.put(key, new ConfigCacheEntry(value, System.currentTimeMillis() + CACHE_TTL_MS));
        return value;
    }

    private String getAppId() { return getConfigValue("feishu_app_id"); }
    private String getAppSecret() { return getConfigValue("feishu_app_secret"); }
    private String getSpreadsheetToken() { return getConfigValue("feishu_spreadsheet_token"); }

    public boolean isConfigured() {
        return !getAppId().isEmpty() && !getAppSecret().isEmpty() && !getSpreadsheetToken().isEmpty();
    }

    private String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() / 1000 < tokenExpireAt) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && System.currentTimeMillis() / 1000 < tokenExpireAt) {
                return cachedToken;
            }
            Exception lastException = null;
            for (int i = 0; i < 3; i++) {
                try {
                    if (i > 0) Thread.sleep(1000L);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    ObjectNode body = objectMapper.createObjectNode();
                    body.put("app_id", getAppId());
                    body.put("app_secret", getAppSecret());
                    HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
                    ResponseEntity<JsonNode> response = restTemplate.exchange(
                            BASE_URL + "/auth/v3/tenant_access_token/internal",
                            HttpMethod.POST, request, JsonNode.class);
                    JsonNode resp = response.getBody();
                    if (resp != null && resp.has("code") && resp.get("code").asInt() == 0) {
                        cachedToken = resp.get("tenant_access_token").asText();
                        int expire = resp.get("expire").asInt();
                        tokenExpireAt = System.currentTimeMillis() / 1000 + expire - TOKEN_EXPIRE_BUFFER;
                        return cachedToken;
                    }
                    throw new RuntimeException("获取飞书 token 失败: " + (resp != null ? resp.toString() : "null"));
                } catch (Exception e) {
                    lastException = e;
                    log.warn("获取飞书 token 重试 {}/3: {}", i + 1, e.getMessage());
                }
            }
            throw new RuntimeException("获取飞书 token 异常，已重试 3 次: " + lastException.getMessage());
        }
    }

    private <T> T executeWithRetry(String url, HttpMethod method, HttpEntity<?> request,
                                    Class<T> responseType, int maxRetries) {
        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (i > 0) Thread.sleep(1000L * i);
                ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
                return response.getBody();
            } catch (Exception e) {
                lastException = e;
                log.warn("飞书 API 调用重试 {}/{}: {}", i + 1, maxRetries, e.getMessage());
            }
        }
        throw new RuntimeException("飞书 API 调用失败，已重试 " + maxRetries + " 次: " + lastException.getMessage());
    }

    public void writeRows(String sheetTitle, String startRange, List<String> headers, List<List<Object>> rows) {
        if (!isConfigured()) {
            throw new RuntimeException("飞书配置未完善，请在规则配置中设置飞书参数");
        }
        String token = getAccessToken();
        String spreadsheetToken = getSpreadsheetToken();

        String sheetId = getSheetId(token, spreadsheetToken, sheetTitle);
        if (sheetId == null) {
            throw new RuntimeException("找不到 sheet: " + sheetTitle);
        }

        int totalRows = 1 + rows.size();
        int totalCols = headers.size();
        String endCol = getColumnLetter(totalCols);

        // 先清空 sheet 中所有已有数据
        clearSheet(token, spreadsheetToken, sheetId, totalCols);

        // 写入新数据
        String range = sheetId + "!" + startRange + ":" + endCol + totalRows;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(token);

        ArrayNode values = objectMapper.createArrayNode();
        ArrayNode headerRow = objectMapper.createArrayNode();
        for (String h : headers) {
            headerRow.add(h != null ? h : "");
        }
        values.add(headerRow);
        for (List<Object> row : rows) {
            ArrayNode dataRow = objectMapper.createArrayNode();
            for (Object cell : row) {
                dataRow.add(cell != null ? cell.toString() : "");
            }
            values.add(dataRow);
        }

        ObjectNode valueRange = objectMapper.createObjectNode();
        valueRange.put("range", range);
        valueRange.set("values", values);

        ObjectNode finalBody = objectMapper.createObjectNode();
        finalBody.set("valueRange", valueRange);

        try {
            HttpEntity<String> request = new HttpEntity<>(finalBody.toString(), httpHeaders);
            log.info("飞书写入请求: {}", finalBody.toString());
            JsonNode resp = executeWithRetry(
                    BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values",
                    HttpMethod.PUT, request, JsonNode.class, 3);
            if (resp != null && resp.has("code") && resp.get("code").asInt() == 0) {
                log.info("飞书写入成功，spreadsheet={}, range={}", spreadsheetToken, range);
            } else {
                throw new RuntimeException("飞书写入失败: " + (resp != null ? resp.toString() : "null"));
            }
        } catch (Exception e) {
            log.error("飞书写入异常", e);
            throw new RuntimeException("飞书写入异常: " + e.getMessage());
        }
    }

    private void clearSheet(String token, String spreadsheetToken, String sheetId, int totalCols) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(token);

        // 获取 sheet 实际数据范围（最大行数）
        int maxRows = getSheetMaxRows(token, spreadsheetToken, sheetId);
        if (maxRows <= 1) {
            return; // 没有数据或只有标题行，无需清空
        }

        String endCol = getColumnLetter(totalCols);
        String range = sheetId + "!A1:" + endCol + maxRows;

        // 用空值覆盖整个范围来清空数据
        ArrayNode emptyValues = objectMapper.createArrayNode();
        for (int r = 0; r < maxRows; r++) {
            ArrayNode emptyRow = objectMapper.createArrayNode();
            for (int c = 0; c < totalCols; c++) {
                emptyRow.add("");
            }
            emptyValues.add(emptyRow);
        }

        ObjectNode valueRange = objectMapper.createObjectNode();
        valueRange.put("range", range);
        valueRange.set("values", emptyValues);

        ObjectNode finalBody = objectMapper.createObjectNode();
        finalBody.set("valueRange", valueRange);

        try {
            HttpEntity<String> request = new HttpEntity<>(finalBody.toString(), httpHeaders);
            JsonNode resp = executeWithRetry(
                    BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values",
                    HttpMethod.PUT, request, JsonNode.class, 3);
            if (resp != null && resp.has("code") && resp.get("code").asInt() == 0) {
                log.info("飞书清空成功，spreadsheet={}, range={}", spreadsheetToken, range);
            } else {
                log.warn("飞书清空返回异常: {}", resp != null ? resp.toString() : "null");
            }
        } catch (Exception e) {
            log.warn("飞书清空失败，继续写入: {}", e.getMessage());
        }
    }

    private int getSheetMaxRows(String token, String spreadsheetToken, String sheetId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> req = new HttpEntity<>(headers);
        try {
            JsonNode body = executeWithRetry(
                    BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/metainfo",
                    HttpMethod.GET, req, JsonNode.class, 3);
            if (body != null && body.has("code") && body.get("code").asInt() == 0) {
                JsonNode sheets = body.get("data").get("sheets");
                for (JsonNode sheet : sheets) {
                    if (sheet.has("sheetId") && sheet.get("sheetId").asText().equals(sheetId)) {
                        if (sheet.has("rowCount")) {
                            return sheet.get("rowCount").asInt();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取 sheet 行数失败: {}", e.getMessage());
        }
        return 0;
    }

    private String getSheetId(String token, String spreadsheetToken, String sheetTitle) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> req = new HttpEntity<>(headers);
        try {
            JsonNode body = executeWithRetry(
                    BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/metainfo",
                    HttpMethod.GET, req, JsonNode.class, 3);
            if (body != null && body.has("code") && body.get("code").asInt() == 0) {
                JsonNode sheets = body.get("data").get("sheets");
                for (JsonNode sheet : sheets) {
                    if (sheetTitle.equals(sheet.get("title").asText())) {
                        return sheet.get("sheetId").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取 sheet 元信息异常", e);
            throw new RuntimeException("获取 sheet 元信息失败: " + e.getMessage());
        }
        return null;
    }

    private String getColumnLetter(int col) {
        StringBuilder sb = new StringBuilder();
        while (col > 0) {
            col--;
            sb.insert(0, (char) ('A' + col % 26));
            col /= 26;
        }
        return sb.toString();
    }
}