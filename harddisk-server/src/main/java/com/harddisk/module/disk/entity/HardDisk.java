package com.harddisk.module.disk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harddisk.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hard_disk")
public class HardDisk extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer displaySeq;
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

    @TableLogic
    private Integer deleted;
}