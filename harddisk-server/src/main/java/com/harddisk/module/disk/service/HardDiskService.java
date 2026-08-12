package com.harddisk.module.disk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harddisk.module.disk.dto.*;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.mapper.HardDiskMapper;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HardDiskService {

    private final HardDiskMapper hardDiskMapper;
    private final DiskUsageRecordMapper usageRecordMapper;
    private final SysUserMapper sysUserMapper;

    public Page<HardDisk> list(int page, int pageSize, String model, String sn, Boolean isIdle) {
        Page<HardDisk> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<HardDisk> wrapper = new LambdaQueryWrapper<HardDisk>()
                .like(model != null, HardDisk::getModel, model)
                .like(sn != null, HardDisk::getSn, sn)
                .eq(isIdle != null, HardDisk::getIsIdle, isIdle)
                .orderByDesc(HardDisk::getCreateTime);
        return hardDiskMapper.selectPage(p, wrapper);
    }

    public HardDisk getById(Long id) {
        HardDisk disk = hardDiskMapper.selectById(id);
        if (disk == null) throw new IllegalArgumentException("硬盘不存在");
        return disk;
    }

    @Transactional
    public HardDisk create(HardDiskCreateRequest req, Long userId) {
        HardDisk disk = new HardDisk();
        disk.setModel(req.getModel());
        disk.setSn(req.getSn());
        disk.setCapacity(req.getCapacity());
        disk.setLocation(req.getLocation());
        disk.setPurchaseTime(req.getPurchaseTime());
        disk.setPurchasePrice(req.getPurchasePrice());
        disk.setPurchaseOaNo(req.getPurchaseOaNo());
        disk.setRemark(req.getRemark());
        disk.setCreatorId(userId);
        disk.setIsIdle(true);
        hardDiskMapper.insert(disk);

        DiskUsageRecord record = new DiskUsageRecord();
        record.setDiskId(disk.getId());
        record.setStatus(4);
        record.setInTime(LocalDateTime.now());
        record.setOperatorId(userId);
        usageRecordMapper.insert(record);

        disk.setCurrentRecordId(record.getId());
        hardDiskMapper.updateById(disk);

        return disk;
    }

    @Transactional
    public HardDisk update(Long id, HardDiskUpdateRequest req) {
        HardDisk disk = getById(id);
        if (req.getModel() != null) disk.setModel(req.getModel());
        if (req.getSn() != null) disk.setSn(req.getSn());
        if (req.getCapacity() != null) disk.setCapacity(req.getCapacity());
        if (req.getLocation() != null) disk.setLocation(req.getLocation());
        if (req.getPurchaseTime() != null) disk.setPurchaseTime(req.getPurchaseTime());
        if (req.getPurchasePrice() != null) disk.setPurchasePrice(req.getPurchasePrice());
        if (req.getPurchaseOaNo() != null) disk.setPurchaseOaNo(req.getPurchaseOaNo());
        if (req.getRemark() != null) disk.setRemark(req.getRemark());
        hardDiskMapper.updateById(disk);
        return disk;
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        hardDiskMapper.deleteById(id);
    }

    @Transactional
    public DiskUsageRecord outbound(DiskOutRequest req, Long userId) {
        HardDisk disk = getById(req.getDiskId());
        if (!disk.getIsIdle()) {
            throw new IllegalArgumentException("硬盘当前不可用，状态不是空闲");
        }

        DiskUsageRecord parentRecord = null;
        if (disk.getCurrentRecordId() != null) {
            parentRecord = usageRecordMapper.selectById(disk.getCurrentRecordId());
        }

        DiskUsageRecord record = new DiskUsageRecord();
        record.setDiskId(req.getDiskId());
        record.setStatus(1);
        record.setOutTime(req.getOutTime() != null ? req.getOutTime() : LocalDateTime.now());
        record.setStorageContent(req.getStorageContent());
        record.setOperatorId(userId);
        record.setParentRecordId(parentRecord != null ? parentRecord.getId() : null);
        usageRecordMapper.insert(record);

        disk.setIsIdle(false);
        disk.setCurrentRecordId(record.getId());
        hardDiskMapper.updateById(disk);
        return record;
    }

    @Transactional
    public DiskUsageRecord inbound(DiskInRequest req, Long userId) {
        DiskUsageRecord record = usageRecordMapper.selectById(req.getRecordId());
        if (record == null) throw new IllegalArgumentException("使用记录不存在");
        if (record.getStatus() != 1 && record.getStatus() != 2 && record.getStatus() != 3) {
            throw new IllegalArgumentException("当前记录状态不允许入库操作");
        }

        int newStatus = req.getStatus() != null ? req.getStatus() : 3;
        record.setStatus(newStatus);
        record.setInTime(req.getInTime() != null ? req.getInTime() : LocalDateTime.now());
        if (req.getStorageContent() != null) record.setStorageContent(req.getStorageContent());
        usageRecordMapper.updateById(record);

        HardDisk disk = hardDiskMapper.selectById(record.getDiskId());
        if (newStatus == 4) {
            disk.setIsIdle(true);
        }
        hardDiskMapper.updateById(disk);
        return record;
    }

    public List<DiskUsageRecord> getRecords(Long diskId) {
        return usageRecordMapper.selectList(
                new LambdaQueryWrapper<DiskUsageRecord>()
                        .eq(DiskUsageRecord::getDiskId, diskId)
                        .orderByDesc(DiskUsageRecord::getCreateTime));
    }

    public DiskUsageRecord getRecordById(Long recordId) {
        DiskUsageRecord record = usageRecordMapper.selectById(recordId);
        if (record == null) throw new IllegalArgumentException("使用记录不存在");
        return record;
    }

    // ========== 记录管理 CRUD ==========

    public Page<DiskUsageRecord> listAllRecords(int page, int pageSize, Long recordId, String model, String sn, String operatorName, String storageContent, Integer status) {
        // 1. 根据型号/SN模糊查询硬盘ID
        List<Long> matchedDiskIds = null;
        if (model != null || sn != null) {
            List<HardDisk> matchedDisks = hardDiskMapper.selectList(
                    new LambdaQueryWrapper<HardDisk>()
                            .like(model != null, HardDisk::getModel, model)
                            .like(sn != null, HardDisk::getSn, sn));
            matchedDiskIds = matchedDisks.stream().map(HardDisk::getId).collect(Collectors.toList());
            if (matchedDiskIds.isEmpty()) {
                return new Page<>(page, pageSize);
            }
        }

        // 2. 根据操作人用户名模糊查询用户ID
        List<Long> matchedUserIds = null;
        if (operatorName != null) {
            List<SysUser> matchedUsers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getUsername, operatorName));
            matchedUserIds = matchedUsers.stream().map(SysUser::getId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                return new Page<>(page, pageSize);
            }
        }

        // 3. 查询使用记录
        Page<DiskUsageRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<DiskUsageRecord> wrapper = new LambdaQueryWrapper<DiskUsageRecord>()
                .eq(recordId != null, DiskUsageRecord::getId, recordId)
                .in(matchedDiskIds != null && !matchedDiskIds.isEmpty(), DiskUsageRecord::getDiskId, matchedDiskIds)
                .in(matchedUserIds != null && !matchedUserIds.isEmpty(), DiskUsageRecord::getOperatorId, matchedUserIds)
                .like(storageContent != null, DiskUsageRecord::getStorageContent, storageContent)
                .eq(status != null, DiskUsageRecord::getStatus, status)
                .orderByDesc(DiskUsageRecord::getCreateTime);
        Page<DiskUsageRecord> result = usageRecordMapper.selectPage(p, wrapper);

        // 4. 填充关联信息
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
                }
                record.setOperatorName(userNameMap.get(record.getOperatorId()));
            }
        }
        return result;
    }

    @Transactional
    public DiskUsageRecord updateRecord(Long id, RecordUpdateRequest req) {
        DiskUsageRecord record = usageRecordMapper.selectById(id);
        if (record == null) throw new IllegalArgumentException("使用记录不存在");

        if (req.getStatus() != null) record.setStatus(req.getStatus());
        if (req.getOutTime() != null) record.setOutTime(req.getOutTime());
        if (req.getInTime() != null) record.setInTime(req.getInTime());
        if (req.getStorageContent() != null) record.setStorageContent(req.getStorageContent());
        usageRecordMapper.updateById(record);

        if (req.getStatus() != null && req.getStatus() == 4) {
            HardDisk disk = hardDiskMapper.selectById(record.getDiskId());
            if (disk != null) {
                disk.setIsIdle(true);
                hardDiskMapper.updateById(disk);
            }
        }
        return record;
    }

    @Transactional
    public void deleteRecord(Long id) {
        DiskUsageRecord record = usageRecordMapper.selectById(id);
        if (record == null) throw new IllegalArgumentException("使用记录不存在");
        usageRecordMapper.deleteById(id);
    }
}
