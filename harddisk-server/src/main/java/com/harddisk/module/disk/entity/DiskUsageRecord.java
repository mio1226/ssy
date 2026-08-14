package com.harddisk.module.disk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("disk_usage_record")
public class DiskUsageRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer displaySeq;
    private Long diskId;
    private Integer status;          // 1=出库 3=入库待备份 4=入库已备份
    private LocalDateTime outTime;
    private LocalDateTime inTime;
    private String storageContent;
    private Long operatorId;
    private Long parentRecordId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    // 关联硬盘信息（非数据库字段）
    @TableField(exist = false)
    private Integer diskDisplaySeq;

    @TableField(exist = false)
    private String diskModel;

    @TableField(exist = false)
    private String diskSn;

    @TableField(exist = false)
    private String operatorName;
}