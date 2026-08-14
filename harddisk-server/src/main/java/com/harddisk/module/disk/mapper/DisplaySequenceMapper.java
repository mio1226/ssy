package com.harddisk.module.disk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harddisk.module.disk.entity.DisplaySequence;
import org.apache.ibatis.annotations.Select;

public interface DisplaySequenceMapper extends BaseMapper<DisplaySequence> {

    @Select("SELECT * FROM display_sequence WHERE seq_name = #{seqName} FOR UPDATE")
    DisplaySequence selectForUpdate(String seqName);
}