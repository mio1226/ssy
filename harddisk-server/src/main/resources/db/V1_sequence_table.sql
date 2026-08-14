-- 序号生成专用表，用于解决并发场景下的 display_seq 冲突
CREATE TABLE IF NOT EXISTS display_sequence (
    seq_name VARCHAR(50) PRIMARY KEY COMMENT '序列名称',
    next_val BIGINT NOT NULL DEFAULT 1 COMMENT '下一个可用值'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='序号生成器';

-- 初始化默认序列
INSERT INTO display_sequence (seq_name, next_val) VALUES ('hard_disk', 1), ('disk_usage_record', 1)
ON DUPLICATE KEY UPDATE next_val = next_val;
