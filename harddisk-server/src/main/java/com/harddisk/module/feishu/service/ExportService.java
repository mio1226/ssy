package com.harddisk.module.feishu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.springframework.transaction.annotation.Transactional;

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
    private static final int BATCH_SIZE = 1000;

    @Transactional(readOnly = true)
    public void exportDisks(String sheetId) {
        List<String> headers = List.of("序号", "型号", "SN码", "容量(TB)", "位置", "采购时间",
                "采购单价", "采购OA流程编号", "备注", "状态", "创建时间");

        List<List<Object>> allRows = new ArrayList<>();
        long page = 1;
        long total;
        do {
            Page<HardDisk> p = new Page<>(page, BATCH_SIZE);
            Page<HardDisk> result = hardDiskMapper.selectPage(p,
                    new LambdaQueryWrapper<HardDisk>().orderByDesc(HardDisk::getCreateTime));
            total = result.getTotal();
            for (HardDisk d : result.getRecords()) {
                List<Object> row = new ArrayList<>();
                row.add(d.getDisplaySeq());
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
                allRows.add(row);
            }
            page++;
            log.info("导出硬盘数据: 已处理 {}/{} 条", allRows.size(), total);
        } while (allRows.size() < total);

        feishuService.writeRows(sheetId, "A1", headers, allRows);
        log.info("硬盘数据导出完成，共 {} 条", allRows.size());
    }

    @Transactional(readOnly = true)
    public void exportRecords(String sheetId) {
        List<String> headers = List.of("序号", "硬盘序号", "硬盘型号", "SN码", "磁盘容量(TB)", "硬盘位置",
                "出库时间", "入库时间", "使用状态", "是否空闲", "存储信息内容",
                "采购时间", "采购单价(人民币)", "采购OA流程编号", "备注", "创建人", "父记录序号");

        List<List<Object>> allRows = new ArrayList<>();
        long page = 1;
        long total;
        do {
            Page<DiskUsageRecord> p = new Page<>(page, BATCH_SIZE);
            Page<DiskUsageRecord> result = usageRecordMapper.selectPage(p,
                    new LambdaQueryWrapper<DiskUsageRecord>().orderByDesc(DiskUsageRecord::getCreateTime));
            total = result.getTotal();

            List<Long> diskIds = result.getRecords().stream()
                    .map(DiskUsageRecord::getDiskId).distinct().collect(Collectors.toList());
            List<Long> userIds = result.getRecords().stream()
                    .map(DiskUsageRecord::getOperatorId).distinct().collect(Collectors.toList());
            List<Long> parentIds = result.getRecords().stream()
                    .map(DiskUsageRecord::getParentRecordId).filter(id -> id != null).distinct().collect(Collectors.toList());

            Map<Long, HardDisk> diskMap = diskIds.isEmpty() ? Map.of() : hardDiskMapper.selectBatchIds(diskIds).stream()
                    .collect(Collectors.toMap(HardDisk::getId, d -> d));
            Map<Long, String> userMap = userIds.isEmpty() ? Map.of() : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
            Map<Long, DiskUsageRecord> parentMap = parentIds.isEmpty() ? Map.of() : usageRecordMapper.selectBatchIds(parentIds).stream()
                    .collect(Collectors.toMap(DiskUsageRecord::getId, r -> r));

            for (DiskUsageRecord r : result.getRecords()) {
                HardDisk disk = diskMap.get(r.getDiskId());
                DiskUsageRecord parent = r.getParentRecordId() != null ? parentMap.get(r.getParentRecordId()) : null;
                List<Object> row = new ArrayList<>();
                row.add(r.getDisplaySeq());
                row.add(disk != null ? disk.getDisplaySeq() : "");
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
                row.add(parent != null ? parent.getDisplaySeq() : "");
                allRows.add(row);
            }
            page++;
            log.info("导出使用记录: 已处理 {}/{} 条", allRows.size(), total);
        } while (allRows.size() < total);

        feishuService.writeRows(sheetId, "A1", headers, allRows);
        log.info("使用记录导出完成，共 {} 条", allRows.size());
    }

    @Transactional(readOnly = true)
    public void exportViolations(String sheetId) {
        List<String> headers = List.of("硬盘序号", "记录序号", "用户", "硬盘型号", "硬盘SN", "采购OA流程编号", "类型",
                "描述", "状态", "创建时间", "处理时间");

        List<List<Object>> allRows = new ArrayList<>();
        long page = 1;
        long total;
        do {
            Page<ViolationRecord> p = new Page<>(page, BATCH_SIZE);
            Page<ViolationRecord> result = violationRecordMapper.selectPage(p,
                    new LambdaQueryWrapper<ViolationRecord>().orderByDesc(ViolationRecord::getCreateTime));
            total = result.getTotal();

            List<Long> diskIds = result.getRecords().stream()
                    .map(ViolationRecord::getDiskId).distinct().collect(Collectors.toList());
            List<Long> userIds = result.getRecords().stream()
                    .map(ViolationRecord::getUserId).distinct().collect(Collectors.toList());
            List<Long> recordIds = result.getRecords().stream()
                    .filter(v -> v.getRecordId() != null)
                    .map(ViolationRecord::getRecordId).distinct().collect(Collectors.toList());

            Map<Long, HardDisk> diskMap = diskIds.isEmpty() ? Map.of() : hardDiskMapper.selectBatchIds(diskIds).stream()
                    .collect(Collectors.toMap(HardDisk::getId, d -> d));
            Map<Long, String> userMap = userIds.isEmpty() ? Map.of() : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
            Map<Long, DiskUsageRecord> recMap = recordIds.isEmpty() ? Map.of() : usageRecordMapper.selectBatchIds(recordIds).stream()
                    .collect(Collectors.toMap(DiskUsageRecord::getId, r -> r));

            for (ViolationRecord v : result.getRecords()) {
                HardDisk disk = diskMap.get(v.getDiskId());
                DiskUsageRecord rec = v.getRecordId() != null ? recMap.get(v.getRecordId()) : null;
                List<Object> row = new ArrayList<>();
                row.add(disk != null ? disk.getDisplaySeq() : "");
                row.add(rec != null ? rec.getDisplaySeq() : "");
                row.add(userMap.getOrDefault(v.getUserId(), ""));
                row.add(disk != null ? disk.getModel() : "");
                row.add(disk != null ? disk.getSn() : "");
                row.add(disk != null ? disk.getPurchaseOaNo() : "");
                row.add("timeout".equals(v.getType()) ? "超时" : "重复使用");
                row.add(v.getDescription());
                row.add(v.getStatus() != null && v.getStatus() == 0 ? "待处理" : "已处理");
                row.add(fmt(v.getCreateTime()));
                row.add(fmt(v.getHandledTime()));
                allRows.add(row);
            }
            page++;
            log.info("导出违规记录: 已处理 {}/{} 条", allRows.size(), total);
        } while (allRows.size() < total);

        feishuService.writeRows(sheetId, "A1", headers, allRows);
        log.info("违规记录导出完成，共 {} 条", allRows.size());
    }

    private String fmt(LocalDateTime dt) {
        return dt != null ? dt.format(DTF) : "";
    }

    private String statusLabel(int status) {
        switch (status) {
            case 1: return "出库";
            case 2: return "未知状态";
            case 3: return "入库待备份";
            case 4: return "入库已备份";
            default: return "未知";
        }
    }
}
