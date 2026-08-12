package com.harddisk.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harddisk.module.dashboard.dto.DashboardStats;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.disk.mapper.HardDiskMapper;
import com.harddisk.module.rule.entity.ViolationRecord;
import com.harddisk.module.rule.mapper.ViolationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final HardDiskMapper hardDiskMapper;
    private final DiskUsageRecordMapper usageRecordMapper;
    private final ViolationRecordMapper violationRecordMapper;

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        // 硬盘统计
        stats.setTotalDisks(hardDiskMapper.selectCount(null));
        stats.setIdleDisks(hardDiskMapper.selectCount(
                new LambdaQueryWrapper<HardDisk>().eq(HardDisk::getIsIdle, true)));
        stats.setInUseDisks(hardDiskMapper.selectCount(
                new LambdaQueryWrapper<HardDisk>().eq(HardDisk::getIsIdle, false)));

        // 使用记录统计
        stats.setTotalRecords(usageRecordMapper.selectCount(null));
        stats.setOutboundRecords(usageRecordMapper.selectCount(
                new LambdaQueryWrapper<DiskUsageRecord>().eq(DiskUsageRecord::getStatus, 1)));
        stats.setStoringRecords(usageRecordMapper.selectCount(
                new LambdaQueryWrapper<DiskUsageRecord>().eq(DiskUsageRecord::getStatus, 2)));
        stats.setInboundPendingRecords(usageRecordMapper.selectCount(
                new LambdaQueryWrapper<DiskUsageRecord>().eq(DiskUsageRecord::getStatus, 3)));
        stats.setInboundDoneRecords(usageRecordMapper.selectCount(
                new LambdaQueryWrapper<DiskUsageRecord>().eq(DiskUsageRecord::getStatus, 4)));

        // 本月统计
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        stats.setMonthOutboundCount(usageRecordMapper.selectCount(
                new LambdaQueryWrapper<DiskUsageRecord>()
                        .eq(DiskUsageRecord::getStatus, 1)
                        .ge(DiskUsageRecord::getOutTime, monthStart)));
        stats.setMonthInboundCount(usageRecordMapper.selectCount(
                new LambdaQueryWrapper<DiskUsageRecord>()
                        .in(DiskUsageRecord::getStatus, 3, 4)
                        .ge(DiskUsageRecord::getInTime, monthStart)));

        // 违规统计
        stats.setTotalViolations(violationRecordMapper.selectCount(null));
        stats.setPendingViolations(violationRecordMapper.selectCount(
                new LambdaQueryWrapper<ViolationRecord>().eq(ViolationRecord::getStatus, 0)));

        return stats;
    }
}
