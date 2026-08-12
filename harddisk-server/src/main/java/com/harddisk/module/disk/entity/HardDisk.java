package com.harddisk.module.disk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hard_disk")
public class HardDisk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String model;
    private String sn;
    private BigDecimal capacity;
    private String location;
    private LocalDateTime purchaseTime;
    private BigDecimal purchasePrice;
    private String purchaseOaNo;
    private String remark;
    private Long creatorId;
    private Boolean isIdle;
    private Long currentRecordId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
