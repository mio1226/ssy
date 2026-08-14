package com.harddisk.module.feishu.controller;

import com.harddisk.common.Result;
import com.harddisk.module.feishu.service.ExportService;
import com.harddisk.module.feishu.service.FeishuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/export/disks")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> exportDisks(@RequestParam(defaultValue = "Sheet1") String sheetId) {
        exportService.exportDisks(sheetId);
        return Result.success();
    }

    @PostMapping("/export/records")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> exportRecords(@RequestParam(defaultValue = "Sheet1") String sheetId) {
        exportService.exportRecords(sheetId);
        return Result.success();
    }

    @PostMapping("/export/violations")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> exportViolations(@RequestParam(defaultValue = "Sheet1") String sheetId) {
        exportService.exportViolations(sheetId);
        return Result.success();
    }
}