package com.harddisk.module.disk.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecordUpdateRequest {
    private Integer status;
    private LocalDateTime outTime;
    private LocalDateTime inTime;
    private String storageContent;
}