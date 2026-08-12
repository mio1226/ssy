package com.harddisk.module.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rule_config")
public class RuleConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleKey;
    private String ruleValue;
    private String description;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
