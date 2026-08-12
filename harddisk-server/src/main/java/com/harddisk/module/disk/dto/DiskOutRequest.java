package com.harddisk.module.disk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DiskOutRequest {
    @NotNull(message = "硬盘ID不能为空")
    private Long diskId;
    private String storageContent;
    private LocalDateTime outTime;
}
