package com.harddisk.module.disk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.harddisk.module.disk.entity.HardDisk;
import org.apache.ibatis.annotations.Update;

public interface HardDiskMapper extends BaseMapper<HardDisk> {

    @Update("UPDATE hard_disk h JOIN (SELECT id, @row := @row + 1 AS new_seq FROM hard_disk, (SELECT @row := 0) r WHERE deleted = 0 ORDER BY id) seq ON h.id = seq.id SET h.display_seq = seq.new_seq")
    void renumberDisplaySeq();

    @Update("UPDATE display_sequence SET next_val = (SELECT COALESCE(MAX(display_seq), 0) + 1 FROM hard_disk WHERE deleted = 0) WHERE seq_name = 'hard_disk'")
    void syncDisplaySequence();
}