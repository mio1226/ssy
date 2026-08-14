package com.harddisk.module.disk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harddisk.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("disk_usage_record")
public class DiskUsageRecord extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer displaySeq;
    private Long diskId;
    private Integer status;
    private LocalDateTime outTime;
    private LocalDateTime inTime;
    private String storageContent;
    private Long operatorId;
    private Long parentRecordId;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Integer diskDisplaySeq;

    @TableField(exist = false)
    private String diskModel;

    @TableField(exist = false)
    private String diskSn;

    @TableField(exist = false)
    private String operatorName;
}