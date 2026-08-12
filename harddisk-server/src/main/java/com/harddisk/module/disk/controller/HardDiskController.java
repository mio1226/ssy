package com.harddisk.module.disk.controller;

import com.harddisk.common.Result;
import com.harddisk.common.PageResult;
import com.harddisk.module.disk.dto.*;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.service.HardDiskService;
import com.harddisk.module.auth.entity.SysUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disk")
@RequiredArgsConstructor
public class HardDiskController {

    private final HardDiskService hardDiskService;

    @GetMapping("/list")
    public Result<PageResult<HardDisk>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) Boolean isIdle) {
        var p = hardDiskService.list(page, pageSize, model, sn, isIdle);
        return Result.success(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<HardDisk> getById(@PathVariable Long id) {
        return Result.success(hardDiskService.getById(id));
    }

    @PostMapping
    public Result<HardDisk> create(@Valid @RequestBody HardDiskCreateRequest req,
                                    @AuthenticationPrincipal SysUser user) {
        return Result.success(hardDiskService.create(req, user.getId()));
    }

    @PutMapping("/{id}")
    public Result<HardDisk> update(@PathVariable Long id, @RequestBody HardDiskUpdateRequest req) {
        return Result.success(hardDiskService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        hardDiskService.delete(id);
        return Result.success();
    }

    @PostMapping("/outbound")
    public Result<DiskUsageRecord> outbound(@Valid @RequestBody DiskOutRequest req,
                                             @AuthenticationPrincipal SysUser user) {
        return Result.success(hardDiskService.outbound(req, user.getId()));
    }

    @PostMapping("/inbound")
    public Result<DiskUsageRecord> inbound(@Valid @RequestBody DiskInRequest req,
                                            @AuthenticationPrincipal SysUser user) {
        return Result.success(hardDiskService.inbound(req, user.getId()));
    }

    @GetMapping("/{diskId}/records")
    public Result<List<DiskUsageRecord>> getRecords(@PathVariable Long diskId) {
        return Result.success(hardDiskService.getRecords(diskId));
    }

    @GetMapping("/records/{recordId}")
    public Result<DiskUsageRecord> getRecord(@PathVariable Long recordId) {
        return Result.success(hardDiskService.getRecordById(recordId));
    }

    // ========== 记录管理 CRUD ==========

    @GetMapping("/records/list")
    public Result<PageResult<DiskUsageRecord>> listAllRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String storageContent,
            @RequestParam(required = false) Integer status) {
        var p = hardDiskService.listAllRecords(page, pageSize, recordId, model, sn, operatorName, storageContent, status);
        return Result.success(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @PutMapping("/records/{id}")
    public Result<DiskUsageRecord> updateRecord(@PathVariable Long id, @RequestBody RecordUpdateRequest req) {
        return Result.success(hardDiskService.updateRecord(id, req));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        hardDiskService.deleteRecord(id);
        return Result.success();
    }
}
