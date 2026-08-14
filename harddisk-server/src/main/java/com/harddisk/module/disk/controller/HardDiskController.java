package com.harddisk.module.disk.controller;

import com.harddisk.common.Result;
import com.harddisk.common.PageResult;
import com.harddisk.module.disk.dto.*;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.service.HardDiskService;
import com.harddisk.module.disk.service.DiskUsageService;
import com.harddisk.module.auth.entity.SysUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disk")
@RequiredArgsConstructor
public class HardDiskController {

    private final HardDiskService hardDiskService;
    private final DiskUsageService diskUsageService;

    @GetMapping("/list")
    public Result<PageResult<HardDisk>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) Boolean isIdle,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        var p = hardDiskService.list(page, pageSize, model, sn, isIdle, sortBy, sortOrder);
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
    @PreAuthorize("hasRole('ADMIN')")
    public Result<HardDisk> update(@PathVariable Long id, @RequestBody HardDiskUpdateRequest req) {
        return Result.success(hardDiskService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal SysUser user) {
        hardDiskService.delete(id, user.getId(), user.getUsername());
        return Result.success();
    }

    @PostMapping("/outbound")
    public Result<DiskUsageRecord> outbound(@Valid @RequestBody DiskOutRequest req,
                                      @AuthenticationPrincipal SysUser user) {
        return Result.success(hardDiskService.outbound(req, user.getId(), user.getUsername()));
    }

    @PostMapping("/inbound")
    public Result<DiskUsageRecord> inbound(@Valid @RequestBody DiskInRequest req,
                                            @AuthenticationPrincipal SysUser user) {
        return Result.success(hardDiskService.inbound(req, user.getId(), user.getUsername()));
    }

    @GetMapping("/{diskId}/records")
    public Result<PageResult<DiskUsageRecord>> getRecords(
            @PathVariable Long diskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        var p = diskUsageService.getRecords(diskId, page, pageSize);
        return Result.success(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @GetMapping("/records/{recordId}")
    public Result<DiskUsageRecord> getRecord(@PathVariable Long recordId) {
        return Result.success(diskUsageService.getRecordById(recordId));
    }

    @GetMapping("/records/list")
    public Result<PageResult<DiskUsageRecord>> listAllRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer displaySeq,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String storageContent,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        var p = diskUsageService.listAllRecords(page, pageSize, displaySeq, model, sn, operatorName, storageContent, status, sortBy, sortOrder);
        return Result.success(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @PutMapping("/records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DiskUsageRecord> updateRecord(@PathVariable Long id, @RequestBody RecordUpdateRequest req, @AuthenticationPrincipal SysUser user) {
        return Result.success(diskUsageService.updateRecord(id, req, user.getId(), user.getUsername()));
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteRecord(@PathVariable Long id, @AuthenticationPrincipal SysUser user) {
        diskUsageService.deleteRecord(id, user.getId(), user.getUsername());
        return Result.success();
    }
}