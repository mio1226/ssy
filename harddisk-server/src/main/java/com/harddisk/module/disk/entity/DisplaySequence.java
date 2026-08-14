package com.harddisk.module.disk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("display_sequence")
public class DisplaySequence {
    @TableId
    private String seqName;
    private Long nextVal;
}