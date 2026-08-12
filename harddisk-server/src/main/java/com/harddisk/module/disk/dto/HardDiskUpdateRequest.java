package com.harddisk.module.disk.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HardDiskUpdateRequest {
    private String model;
    private String sn;
    private BigDecimal capacity;
    private String location;
    private LocalDateTime purchaseTime;
    private BigDecimal purchasePrice;
    private String purchaseOaNo;
    private String remark;
}
