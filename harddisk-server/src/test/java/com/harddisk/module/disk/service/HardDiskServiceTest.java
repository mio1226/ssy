package com.harddisk.module.disk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harddisk.module.disk.dto.HardDiskUpdateRequest;
import com.harddisk.module.disk.entity.DiskUsageRecord;
import com.harddisk.module.disk.entity.HardDisk;
import com.harddisk.module.disk.mapper.DiskUsageRecordMapper;
import com.harddisk.module.disk.mapper.HardDiskMapper;
import com.harddisk.module.auth.mapper.SysUserMapper;
import com.harddisk.module.rule.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HardDiskServiceTest {

    @Mock
    private HardDiskMapper hardDiskMapper;
    @Mock
    private DiskUsageRecordMapper usageRecordMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private RuleService ruleService;

    @Captor
    private ArgumentCaptor<HardDisk> hardDiskCaptor;

    private HardDiskService hardDiskService;

    @BeforeEach
    void setUp() {
        hardDiskService = new HardDiskService(hardDiskMapper, usageRecordMapper, sysUserMapper, ruleService);
    }

    @Test
    @DisplayName("Bug 5.1: update时SN唯一性校验应在字段赋值之前执行")
    void update_shouldValidateSnBeforeSettingFields() {
        HardDisk existingDisk = new HardDisk();
        existingDisk.setId(1L);
        existingDisk.setModel("旧型号");
        existingDisk.setSn("OLD-SN-001");
        existingDisk.setCapacity(new BigDecimal("500"));
        lenient().when(hardDiskMapper.selectById(1L)).thenReturn(existingDisk);

        HardDisk otherDisk = new HardDisk();
        otherDisk.setId(2L);
        otherDisk.setSn("NEW-SN-001");
        when(hardDiskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(otherDisk);

        HardDiskUpdateRequest req = new HardDiskUpdateRequest();
        req.setSn("NEW-SN-001");
        req.setModel("新型号");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> hardDiskService.update(1L, req));
        assertTrue(ex.getMessage().contains("SN码已存在"));

        verify(hardDiskMapper).selectOne(any(LambdaQueryWrapper.class));
        assertEquals("旧型号", existingDisk.getModel());
    }

    @Test
    @DisplayName("Bug 5.2: 删除硬盘后应重置currentRecordId")
    void delete_shouldResetCurrentRecordId() {
        HardDisk disk = new HardDisk();
        disk.setId(1L);
        disk.setSn("SN-001");
        disk.setModel("Model-X");
        disk.setCurrentRecordId(100L);
        disk.setIsIdle(false);
        disk.setDisplaySeq(1);
        when(hardDiskMapper.selectById(1L)).thenReturn(disk);
        when(usageRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // renumberDisks 内部需要 selectList，mock返回空列表避免NPE
        when(hardDiskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        hardDiskService.delete(1L, 1L, "testuser");

        verify(hardDiskMapper).deleteById(1L);
        // 验证至少有一次updateById的入参中currentRecordId为null
        verify(hardDiskMapper, atLeast(1)).updateById(hardDiskCaptor.capture());
        boolean hasReset = hardDiskCaptor.getAllValues().stream()
                .anyMatch(d -> d.getCurrentRecordId() == null);
        assertTrue(hasReset, "应有update将currentRecordId置为null");
    }

    @Test
    @DisplayName("Bug 5.1: SN未变更时不应触发重复校验")
    void update_shouldNotValidateSnWhenUnchanged() {
        HardDisk existingDisk = new HardDisk();
        existingDisk.setId(1L);
        existingDisk.setSn("SAME-SN");
        existingDisk.setModel("旧型号");
        when(hardDiskMapper.selectById(1L)).thenReturn(existingDisk);

        HardDiskUpdateRequest req = new HardDiskUpdateRequest();
        req.setSn("SAME-SN");
        req.setModel("新型号");

        hardDiskService.update(1L, req);

        verify(hardDiskMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        assertEquals("新型号", existingDisk.getModel());
    }
}
