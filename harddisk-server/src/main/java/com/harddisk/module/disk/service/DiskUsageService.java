package com.harddisk.module.disk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harddisk.module.disk.dto.RecordUpdateRequest;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.disk.mapper.HardDiskMapper;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
import com.harddisk.module.rule.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiskUsageService {

    private final DiskUsageRecordMapper usageRecordMapper;
    private final HardDiskMapper hardDiskMapper;
    private final SysUserMapper sysUserMapper;
    private final RuleService ruleService;

    public Page<DiskUsageRecord> getRecords(Long diskId, int page, int pageSize) {
        Page<DiskUsageRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<DiskUsageRecord> wrapper = new LambdaQueryWrapper<DiskUsageRecord>()
                .eq(DiskUsageRecord::getDiskId, diskId)
                .orderByDesc(DiskUsageRecord::getCreateTime);
        return usageRecordMapper.selectPage(p, wrapper);
    }

    public DiskUsageRecord getRecordById(Long recordId) {
        DiskUsageRecord record = usageRecordMapper.selectById(recordId);
        if (record == null) throw new IllegalArgumentException("使用记录不存在");

        HardDisk disk = hardDiskMapper.selectById(record.getDiskId());
        if (disk != null) {
            record.setDiskModel(disk.getModel());
            record.setDiskSn(disk.getSn());
            record.setDiskDisplaySeq(disk.getDisplaySeq());
        }
        SysUser user = sysUserMapper.selectById(record.getOperatorId());
        if (user != null) {
            record.setOperatorName(user.getUsername());
        }
        return record;
    }

    public Page<DiskUsageRecord> listAllRecords(int page, int pageSize, Integer displaySeq,
                                                 String model, String sn, String operatorName,
                                                 String storageContent, Integer status,
                                                 String sortBy, String sortOrder) {
        Page<DiskUsageRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<DiskUsageRecord> wrapper = new LambdaQueryWrapper<DiskUsageRecord>()
                .eq(displaySeq != null, DiskUsageRecord::getDisplaySeq, displaySeq)
                .eq(status != null, DiskUsageRecord::getStatus, status)
                .like(storageContent != null, DiskUsageRecord::getStorageContent, storageContent);

        // 操作人搜索：先查用户表获取 userId，再过滤记录
        if (operatorName != null) {
            List<Long> matchedUserIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getUsername, operatorName)
                            .select(SysUser::getId))
                    .stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return new Page<>(page, pageSize);
            }
            wrapper.in(DiskUsageRecord::getOperatorId, matchedUserIds);
        }

        // 硬盘型号/SN搜索：先查硬盘表获取 diskId，再过滤记录
        if (model != null || sn != null) {
            LambdaQueryWrapper<HardDisk> diskWrapper = new LambdaQueryWrapper<HardDisk>()
                    .like(model != null, HardDisk::getModel, model)
                    .like(sn != null, HardDisk::getSn, sn)
                    .select(HardDisk::getId);
            List<Long> matchedDiskIds = hardDiskMapper.selectList(diskWrapper).stream()
                    .map(HardDisk::getId)
                    .collect(Collectors.toList());
            if (matchedDiskIds.isEmpty()) {
                return new Page<>(page, pageSize);
            }
            wrapper.in(DiskUsageRecord::getDiskId, matchedDiskIds);
        }

        if ("id".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                wrapper.orderByAsc(DiskUsageRecord::getId);
            } else {
                wrapper.orderByDesc(DiskUsageRecord::getId);
            }
        } else if ("disk_id".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                wrapper.orderByAsc(DiskUsageRecord::getDiskId);
            } else {
                wrapper.orderByDesc(DiskUsageRecord::getDiskId);
            }
        } else {
            wrapper.orderByDesc(DiskUsageRecord::getCreateTime);
        }
        Page<DiskUsageRecord> result = usageRecordMapper.selectPage(p, wrapper);

        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            List<Long> diskIds = result.getRecords().stream()
                    .map(DiskUsageRecord::getDiskId)
                    .distinct()
                    .collect(Collectors.toList());
            List<HardDisk> disks = hardDiskMapper.selectBatchIds(diskIds);
            Map<Long, HardDisk> diskMap = disks.stream()
                    .collect(Collectors.toMap(HardDisk::getId, d -> d));

            List<Long> userIds = result.getRecords().stream()
                    .map(DiskUsageRecord::getOperatorId)
                    .distinct()
                    .collect(Collectors.toList());
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            Map<Long, String> userNameMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

            for (DiskUsageRecord record : result.getRecords()) {
                HardDisk disk = diskMap.get(record.getDiskId());
                if (disk != null) {
                    record.setDiskModel(disk.getModel());
                    record.setDiskSn(disk.getSn());
                    record.setDiskDisplaySeq(disk.getDisplaySeq());
                }
                record.setOperatorName(userNameMap.get(record.getOperatorId()));
            }
        }
        return result;
    }

    @Transactional
    public DiskUsageRecord updateRecord(Long id, RecordUpdateRequest req, Long userId, String username) {
        DiskUsageRecord record = usageRecordMapper.selectById(id);
        if (record == null) throw new IllegalArgumentException("使用记录不存在");

        if (req.getStatus() != null) record.setStatus(req.getStatus());
        if (req.getOutTime() != null) record.setOutTime(req.getOutTime());
        if (req.getInTime() != null) record.setInTime(req.getInTime());
        if (req.getStorageContent() != null) record.setStorageContent(req.getStorageContent());
        usageRecordMapper.updateById(record);

        if (req.getStatus() != null) {
            HardDisk disk = hardDiskMapper.selectById(record.getDiskId());
            if (disk != null) {
                if (req.getStatus() == 4) {
                    disk.setIsIdle(true);
                } else if (req.getStatus() == 1) {
                    disk.setIsIdle(false);
                }
                hardDiskMapper.updateById(disk);
            }
        }
        return record;
    }

    @Transactional
    public void deleteRecord(Long id, Long userId, String username) {
        DiskUsageRecord record = usageRecordMapper.selectById(id);
        if (record == null) throw new IllegalArgumentException("使用记录不存在");
        if (record.getStatus() != null && record.getStatus() != 4) {
            ruleService.recordViolation(userId, username, record.getDiskId(), id, "delete_record",
                    "尝试删除非已备份状态的使用记录，当前状态：" + record.getStatus());
            throw new IllegalArgumentException("只能删除已入库已备份的使用记录");
        }
        usageRecordMapper.deleteById(id);
    }
}