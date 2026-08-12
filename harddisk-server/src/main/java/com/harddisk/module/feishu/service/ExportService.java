package com.harddisk.module.feishu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.disk.mapper.HardDiskMapper;
import com.harddisk.module.rule.entity.ViolationRecord;
import com.harddisk.module.rule.mapper.ViolationRecordMapper;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final FeishuService feishuService;
    private final HardDiskMapper hardDiskMapper;
    private final DiskUsageRecordMapper usageRecordMapper;
    private final ViolationRecordMapper violationRecordMapper;
    private final SysUserMapper sysUserMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void exportDisks(String sheetId) {
        List<HardDisk> disks = hardDiskMapper.selectList(
                new LambdaQueryWrapper<HardDisk>().orderByDesc(HardDisk::getCreateTime));

        List<String> headers = List.of("ID", "型号", "SN码", "容量(TB)", "位置", "采购时间",
                "采购单价", "采购OA流程编号", "备注", "状态", "创建时间");

        List<List<Object>> rows = new ArrayList<>();
        for (HardDisk d : disks) {
            List<Object> row = new ArrayList<>();
            row.add(d.getId());
            row.add(d.getModel());
            row.add(d.getSn());
            row.add(d.getCapacity());
            row.add(d.getLocation());
            row.add(fmt(d.getPurchaseTime()));
            row.add(d.getPurchasePrice());
            row.add(d.getPurchaseOaNo());
            row.add(d.getRemark());
            row.add(d.getIsIdle() != null && d.getIsIdle() ? "空闲" : "使用中");
            row.add(fmt(d.getCreateTime()));
            rows.add(row);
        }
        feishuService.writeRows(sheetId, "A1", headers, rows);
    }

    public void exportRecords(String sheetId) {
        List<DiskUsageRecord> records = usageRecordMapper.selectList(
                new LambdaQueryWrapper<DiskUsageRecord>().orderByDesc(DiskUsageRecord::getCreateTime));

        List<Long> diskIds = records.stream().map(DiskUsageRecord::getDiskId).distinct().collect(Collectors.toList());
        List<Long> userIds = records.stream().map(DiskUsageRecord::getOperatorId).distinct().collect(Collectors.toList());

        Map<Long, HardDisk> diskMap = hardDiskMapper.selectBatchIds(diskIds).stream()
                .collect(Collectors.toMap(HardDisk::getId, d -> d));
        Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

        List<String> headers = List.of("记录ID", "硬盘型号", "SN码", "磁盘容量(TB)", "硬盘位置",
                "出库时间", "入库时间", "使用状态", "是否空闲", "存储信息内容",
                "采购时间", "采购单价(人民币)", "采购OA流程编号", "备注", "创建人", "父记录");

        List<List<Object>> rows = new ArrayList<>();
        for (DiskUsageRecord r : records) {
            HardDisk disk = diskMap.get(r.getDiskId());
            List<Object> row = new ArrayList<>();
            row.add(r.getId());
            row.add(disk != null ? disk.getModel() : "");
            row.add(disk != null ? disk.getSn() : "");
            row.add(disk != null ? disk.getCapacity() : "");
            row.add(disk != null ? disk.getLocation() : "");
            row.add(fmt(r.getOutTime()));
            row.add(fmt(r.getInTime()));
            row.add(statusLabel(r.getStatus()));
            row.add(disk != null && disk.getIsIdle() != null && disk.getIsIdle() ? "空闲" : "使用中");
            row.add(r.getStorageContent());
            row.add(fmt(disk != null ? disk.getPurchaseTime() : null));
            row.add(disk != null ? disk.getPurchasePrice() : "");
            row.add(disk != null ? disk.getPurchaseOaNo() : "");
            row.add(disk != null ? disk.getRemark() : "");
            row.add(userMap.getOrDefault(r.getOperatorId(), ""));
            row.add(r.getParentRecordId());
            rows.add(row);
        }
        feishuService.writeRows(sheetId, "A1", headers, rows);
    }

    public void exportViolations(String sheetId) {
        List<ViolationRecord> violations = violationRecordMapper.selectList(
                new LambdaQueryWrapper<ViolationRecord>().orderByDesc(ViolationRecord::getCreateTime));

        List<Long> diskIds = violations.stream().map(ViolationRecord::getDiskId).distinct().collect(Collectors.toList());
        List<Long> userIds = violations.stream().map(ViolationRecord::getUserId).distinct().collect(Collectors.toList());

        Map<Long, HardDisk> diskMap = hardDiskMapper.selectBatchIds(diskIds).stream()
                .collect(Collectors.toMap(HardDisk::getId, d -> d));
        Map<Long, String> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

        List<String> headers = List.of("违规ID", "用户", "硬盘型号", "硬盘SN", "采购OA流程编号", "类型",
                "描述", "状态", "创建时间", "处理时间");

        List<List<Object>> rows = new ArrayList<>();
        for (ViolationRecord v : violations) {
            HardDisk disk = diskMap.get(v.getDiskId());
            List<Object> row = new ArrayList<>();
            row.add(v.getId());
            row.add(userMap.getOrDefault(v.getUserId(), ""));
            row.add(disk != null ? disk.getModel() : "");
            row.add(disk != null ? disk.getSn() : "");
            row.add(disk != null ? disk.getPurchaseOaNo() : "");
            row.add("timeout".equals(v.getType()) ? "超时" : "重复使用");
            row.add(v.getDescription());
            row.add(v.getStatus() != null && v.getStatus() == 0 ? "待处理" : "已处理");
            row.add(fmt(v.getCreateTime()));
            row.add(fmt(v.getHandledTime()));
            rows.add(row);
        }
        feishuService.writeRows(sheetId, "A1", headers, rows);
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DTF) : "";
    }

    private String statusLabel(int status) {
        switch (status) {
            case 1: return "出库";
            case 2: return "存储数据中";
            case 3: return "入库待备份";
            case 4: return "入库已备份";
            default: return "未知";
        }
    }
}

