package com.harddisk.module.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.rule.entity.RuleConfig;
import com.harddisk.module.rule.entity.ViolationRecord;
import com.harddisk.module.rule.mapper.RuleConfigMapper;
import com.harddisk.module.rule.mapper.ViolationRecordMapper;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
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
    private final SysUserMapper sysUserMapper;

    /**
     * 获取规则配置值，不存在或禁用时返回null
     */
    public String getRuleValue(String ruleKey) {
        RuleConfig config = ruleConfigMapper.selectOne(
                new LambdaQueryWrapper<RuleConfig>()
                        .eq(RuleConfig::getRuleKey, ruleKey)
                        .eq(RuleConfig::getStatus, 1));
        return config != null ? config.getRuleValue() : null;
    }

    /**
     * 获取规则配置值并转为 boolean
     */
    public boolean getRuleBoolean(String ruleKey) {
        return "1".equals(getRuleValue(ruleKey));
    }

    /**
     * 记录违规操作
     */
    @Transactional
    public void recordViolation(Long userId, String username, Long diskId, Long recordId, String type, String description) {
        ViolationRecord violation = new ViolationRecord();
        violation.setUserId(userId);
        violation.setUsername(username);
        violation.setDiskId(diskId);
        violation.setRecordId(recordId);
        violation.setType(type);
        violation.setDescription(description);
        violation.setStatus(0);
        violationRecordMapper.insert(violation);
        log.info("违规记录已生成: type={}, userId={}, diskId={}, desc={}", type, userId, diskId, description);
    }

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

            String username = "";
            SysUser user = sysUserMapper.selectById(record.getOperatorId());
            if (user != null) username = user.getUsername();

            recordViolation(record.getOperatorId(), username, record.getDiskId(), record.getId(),
                    "timeout", "硬盘出库超时，出库时间=" + record.getOutTime());
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
            String username = "";
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) username = user.getUsername();

            recordViolation(userId, username, diskId, null, "reuse",
                    "尝试重复使用正在使用中的硬盘");
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
