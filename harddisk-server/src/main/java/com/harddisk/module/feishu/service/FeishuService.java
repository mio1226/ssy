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

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RuleConfigMapper ruleConfigMapper;

    private static final String BASE_URL = "https://open.feishu.cn/open-apis";
    private static final long TOKEN_EXPIRE_BUFFER = 60;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    private String getConfigValue(String key) {
        RuleConfig config = ruleConfigMapper.selectOne(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getRuleKey, key)
                        .eq(RuleConfig::getStatus, 1));
        return config != null ? config.getRuleValue() : "";
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
            // 双重检查，避免并发时多个线程同时获取 token
            if (cachedToken != null && System.currentTimeMillis() / 1000 < tokenExpireAt) {
                return cachedToken;
            }
            try {
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
                throw new RuntimeException("获取飞书 token 异常: " + e.getMessage());
            }
        }
    }

    /**
     * 先获取 sheet 的实际 ID，再写入数据
     */
    public void writeRows(String sheetTitle, String startRange, List<String> headers, List<List<Object>> rows) {
        if (!isConfigured()) {
            throw new RuntimeException("飞书配置未完善，请在规则配置中设置飞书参数");
        }
        String token = getAccessToken();
        String spreadsheetToken = getSpreadsheetToken();

        // 1. 获取 sheet 元信息，找到 sheet 的实际 ID
        String sheetId = getSheetId(token, spreadsheetToken, sheetTitle);
        if (sheetId == null) {
            throw new RuntimeException("找不到 sheet: " + sheetTitle);
        }

        // 2. 写入数据
        int totalRows = 1 + rows.size();
        int totalCols = headers.size();
        String endCol = getColumnLetter(totalCols);
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
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values",
                    HttpMethod.PUT, request, JsonNode.class);
            JsonNode resp = response.getBody();
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

    /**
     * 通过 metainfo 获取 sheet 的实际 ID
     */
    private String getSheetId(String token, String spreadsheetToken, String sheetTitle) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> req = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> resp = restTemplate.exchange(
            BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/metainfo",
            HttpMethod.GET, req, JsonNode.class);
        JsonNode body = resp.getBody();
        if (body != null && body.has("code") && body.get("code").asInt() == 0) {
            JsonNode sheets = body.get("data").get("sheets");
            for (JsonNode sheet : sheets) {
                if (sheetTitle.equals(sheet.get("title").asText())) {
                    return sheet.get("sheetId").asText();
                }
            }
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
