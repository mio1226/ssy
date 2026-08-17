package com.harddisk.module.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.disk.mapper.HardDiskMapper;
import com.harddisk.module.rule.entity.RuleConfig;
import com.harddisk.module.rule.entity.ViolationRecord;
import com.harddisk.module.rule.mapper.RuleConfigMapper;
import com.harddisk.module.rule.mapper.ViolationRecordMapper;
import com.harddisk.module.auth.entity.SysUser;
import com.harddisk.module.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private ViolationRecordMapper violationRecordMapper;
    @Mock
    private RuleConfigMapper ruleConfigMapper;
    @Mock
    private DiskUsageRecordMapper usageRecordMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private HardDiskMapper hardDiskMapper;

    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(violationRecordMapper, ruleConfigMapper, usageRecordMapper, sysUserMapper, hardDiskMapper);
    }

    @Test
    @DisplayName("Bug 5.3: timeout_days=0 应执行超时检查，而非跳过")
    void checkTimeout_shouldProcessWhenTimeoutDaysIsZero() {
        // 模拟配置：timeout_days = "0"
        RuleConfig config = new RuleConfig();
        config.setRuleKey("timeout_days");
        config.setRuleValue("0");
        when(ruleConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        // 模拟有出库记录
        DiskUsageRecord record = new DiskUsageRecord();
        record.setId(1L);
        record.setDiskId(10L);
        record.setOperatorId(100L);
        record.setStatus(1);
        record.setOutTime(LocalDateTime.now().minusDays(1));
        when(usageRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

        // 模拟无已有违规记录
        when(violationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 模拟用户
        SysUser user = new SysUser();
        user.setId(100L);
        user.setUsername("testuser");
        when(sysUserMapper.selectById(100L)).thenReturn(user);

        // 执行
        ruleService.checkTimeout();

        // 验证：生成了违规记录
        verify(violationRecordMapper).insert(any(ViolationRecord.class));
    }

    @Test
    @DisplayName("Bug 5.3: timeout_days 配置为null应跳过检查")
    void checkTimeout_shouldSkipWhenConfigIsNull() {
        when(ruleConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ruleService.checkTimeout();

        // 验证：没有查询出库记录
        verify(usageRecordMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("Bug 5.3: timeout_days 配置为空字符串应跳过检查")
    void checkTimeout_shouldSkipWhenConfigValueIsEmpty() {
        RuleConfig config = new RuleConfig();
        config.setRuleKey("timeout_days");
        config.setRuleValue("");
        when(ruleConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        ruleService.checkTimeout();

        verify(usageRecordMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("数据不完整检查：incomplete_data_check 禁用时应跳过")
    void checkIncompleteData_shouldSkipWhenDisabled() {
        when(ruleConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ruleService.checkIncompleteData();

        verify(hardDiskMapper, never()).selectList(any());
        verify(usageRecordMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("数据不完整检查：硬盘缺少必要字段应生成违规记录")
    void checkIncompleteData_shouldDetectMissingDiskFields() {
        // 模拟配置启用
        when(ruleConfigMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    RuleConfig c = new RuleConfig();
                    c.setRuleKey("incomplete_data_check");
                    c.setRuleValue("1");
                    return c;
                });

        // 模拟硬盘缺少SN码
        HardDisk disk = new HardDisk();
        disk.setId(1L);
        disk.setModel("Test Model");
        disk.setSn("");
        disk.setCapacity(java.math.BigDecimal.valueOf(500));
        disk.setCurrentRecordId(10L);
        when(hardDiskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(disk));

        // 模拟无已有违规记录
        when(violationRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 执行
        ruleService.checkIncompleteData();

        // 验证：生成了违规记录
        verify(violationRecordMapper).insert(argThat(v ->
                "incomplete_data".equals(v.getType()) &&
                v.getDescription().contains("SN码")));
    }
}
