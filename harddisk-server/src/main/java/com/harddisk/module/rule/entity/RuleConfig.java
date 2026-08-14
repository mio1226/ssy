package com.harddisk.module.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.harddisk.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("rule_config")
public class RuleConfig extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleKey;
    private String ruleValue;
    private String description;
    private Integer status;
}