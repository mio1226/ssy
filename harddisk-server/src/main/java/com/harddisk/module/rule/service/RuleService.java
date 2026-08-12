package com.harddisk.module.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.rule.entity.RuleConfig;
import com.harddisk.module.rule.entity.ViolationRecord;
import com.harddisk.module.rule.mapper.RuleConfigMapper;
import com.harddisk.module.rule.mapper.ViolationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final ViolationRecordMapper violationRecordMapper;
    private final RuleConfigMapper ruleConfigMapper;
    private final DiskUsageRecordMapper usageRecordMapper;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkTimeout() {
        RuleConfig config = ruleConfigMapper.selectOne(
                new LambdaQueryWrapper<RuleConfig>().eq(RuleConfig::getRuleKey, "timeout_days"));
        if (config == null || "0".equals(config.getRuleValue())) return;

        int timeoutDays = Integer.parseInt(config.getRuleValue());
        LocalDateTime deadline = LocalDateTime.now().minusDays(timeoutDays);

        List<DiskUsageRecord> timeoutRecords = usageRecordMapper.selectList(
                new LambdaQueryWrapper<DiskUsageRecord>()
                        .eq(DiskUsageRecord::getStatus, 1)
                        .lt(DiskUsageRecord::getOutTime, deadline));

        for (DiskUsageRecord record : timeoutRecords) {
            ViolationRecord existing = violationRecordMapper.selectOne(
                    new LambdaQueryWrapper<ViolationRecord>()
                            .eq(ViolationRecord::getRecordId, record.getId())
                            .eq(ViolationRecord::getType, "timeout"));
            if (existing != null) continue;

            ViolationRecord violation = new ViolationRecord();
            violation.setUserId(record.getOperatorId());
            violation.setDiskId(record.getDiskId());
            violation.setRecordId(record.getId());
            violation.setType("timeout");
            violation.setDescription("硬盘出库超时，出库时间: " + record.getOutTime());
            violation.setStatus(0);
            violationRecordMapper.insert(violation);

            log.info("硬盘ID: {} 出库超时，已生成违规记录", record.getDiskId());
        }
    }

    public void checkReuse(Long diskId, Long userId) {
        List<DiskUsageRecord> activeRecords = usageRecordMapper.selectList(
                new LambdaQueryWrapper<DiskUsageRecord>()
                        .eq(DiskUsageRecord::getDiskId, diskId)
                        .in(DiskUsageRecord::getStatus, 1, 2, 3));

        boolean hasActive = activeRecords.stream()
                .anyMatch(r -> r.getStatus() == 1 || r.getStatus() == 2 || r.getStatus() == 3);

        if (hasActive) {
            ViolationRecord violation = new ViolationRecord();
            violation.setUserId(userId);
            violation.setDiskId(diskId);
            violation.setType("reuse");
            violation.setDescription("尝试重复使用正在使用中的硬盘");
            violation.setStatus(0);
            violationRecordMapper.insert(violation);
            throw new IllegalArgumentException("硬盘当前正在使用中，不可重复出库");
        }
    }

    public Page<ViolationRecord> listViolations(int page, int pageSize, String type, Integer status) {
        Page<ViolationRecord> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<ViolationRecord> wrapper = new LambdaQueryWrapper<ViolationRecord>()
                .eq(type != null, ViolationRecord::getType, type)
                .eq(status != null, ViolationRecord::getStatus, status)
                .orderByDesc(ViolationRecord::getCreateTime);
        return violationRecordMapper.selectPage(p, wrapper);
    }

    @Transactional
    public void resolveViolation(Long id) {
        ViolationRecord record = violationRecordMapper.selectById(id);
        if (record == null) throw new IllegalArgumentException("违规记录不存在");
        record.setStatus(1);
        record.setHandledTime(LocalDateTime.now());
        violationRecordMapper.updateById(record);
    }

    public List<RuleConfig> getRuleConfigs() {
        return ruleConfigMapper.selectList(null);
    }

    @Transactional
    public RuleConfig updateRuleConfig(RuleConfig config) {
        RuleConfig existing = ruleConfigMapper.selectById(config.getId());
        if (existing == null) throw new IllegalArgumentException("规则配置不存在");
        if (config.getRuleValue() != null) existing.setRuleValue(config.getRuleValue());
        if (config.getDescription() != null) existing.setDescription(config.getDescription());
        if (config.getStatus() != null) existing.setStatus(config.getStatus());
        ruleConfigMapper.updateById(existing);
        return existing;
    }
}
