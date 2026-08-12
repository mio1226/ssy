package com.harddisk.module.feishu.controller;

import com.harddisk.common.Result;
import com.harddisk.module.feishu.service.ExportService;
import com.harddisk.module.feishu.service.FeishuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
@RequiredArgsConstructor
public class FeishuExportController {

    private final ExportService exportService;
    private final FeishuService feishuService;

    @GetMapping("/check")
    public Result<Boolean> checkConfig() {
        return Result.success(feishuService.isConfigured());
    }

    @GetMapping("/debug")
    public Result<Object> debug() {
        Map<String, Object> info = new HashMap<>();
        try {
            info.put("configured", feishuService.isConfigured());
            info.put("appId", feishuService.getDebugAppId());
            info.put("spreadsheetToken", feishuService.getDebugSpreadsheetToken());
            info.put("hasSecret", feishuService.getDebugAppSecret() != null && !feishuService.getDebugAppSecret().isEmpty());
            info.put("tokenAcquired", feishuService.getDebugToken() != null);
            info.put("tokenPrefix", feishuService.getDebugToken() != null ? feishuService.getDebugToken().substring(0, Math.min(20, feishuService.getDebugToken().length())) + "..." : "");

            // 尝试获取 sheet 元信息
            info.put("metaInfo", feishuService.debugGetSheetMeta());
        } catch (Exception e) {
            info.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                info.put("cause", e.getCause().getMessage());
            }
        }
        return Result.success(info);
    }

    @PostMapping("/export/disks")
    public Result<Void> exportDisks(@RequestParam(defaultValue = "Sheet1") String sheetId) {
        exportService.exportDisks(sheetId);
        return Result.success();
    }

    @PostMapping("/export/records")
    public Result<Void> exportRecords(@RequestParam(defaultValue = "Sheet1") String sheetId) {
        exportService.exportRecords(sheetId);
        return Result.success();
    }

    @PostMapping("/export/violations")
    public Result<Void> exportViolations(@RequestParam(defaultValue = "Sheet1") String sheetId) {
        exportService.exportViolations(sheetId);
        return Result.success();
    }
}