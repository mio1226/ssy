package com.harddisk.module.disk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HardDiskCreateRequest {
    @NotBlank(message = "硬盘型号不能为空")
    private String model;
    @NotBlank(message = "SN码不能为空")
    private String sn;
    @NotNull(message = "容量不能为空")
    private BigDecimal capacity;
    private String location;
    private LocalDateTime purchaseTime;
    private BigDecimal purchasePrice;
    private String purchaseOaNo;
    private String remark;
}
