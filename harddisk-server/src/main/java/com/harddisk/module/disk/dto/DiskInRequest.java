package com.harddisk.module.disk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DiskInRequest {
    @NotNull(message = "使用记录ID不能为空")
    private Long recordId;
    private Integer status;        // 3=入库待备份 4=入库已备份
    private String storageContent;
    private LocalDateTime inTime;
}
