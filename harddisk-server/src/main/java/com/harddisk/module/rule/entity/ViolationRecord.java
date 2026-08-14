package com.harddisk.module.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("violation_record")
public class ViolationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private Long diskId;
    private Long recordId;
    private String type;             // timeout / reuse / delete_disk_active / delete_record / inbound_invalid_status
    private String description;
    private Integer status;          // 0=pending 1=resolved

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime handledTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
