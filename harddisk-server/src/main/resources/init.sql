-- Update init.sql to include display_seq
-- ============================================
-- 硬盘管理系统 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS harddisk DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE harddisk;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    display_name VARCHAR(100) DEFAULT NULL COMMENT '显示名称',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色 ADMIN/USER',
    status TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0=未删 1=已删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识 ADMIN/USER',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 硬盘表
CREATE TABLE IF NOT EXISTS hard_disk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_seq INT DEFAULT NULL COMMENT '显示序号(删除后重新排序)',
    model VARCHAR(100) NOT NULL COMMENT '硬盘型号(如:希捷/西部/三星等)',
    sn VARCHAR(200) NOT NULL COMMENT 'SN号',
    capacity DECIMAL(10,2) NOT NULL COMMENT '容量(TB)',
    location VARCHAR(255) DEFAULT NULL COMMENT '硬盘位置(如:1号机柜)',
    purchase_time DATETIME DEFAULT NULL COMMENT '采购时间',
    purchase_price DECIMAL(10,2) DEFAULT NULL COMMENT '采购价格(单位:元)',
    purchase_oa_no VARCHAR(100) DEFAULT NULL COMMENT '采购OA号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator_id BIGINT DEFAULT NULL COMMENT '创建者ID',
    is_idle TINYINT(1) DEFAULT 1 COMMENT '是否闲置 0=使用中 1=闲置',
    current_record_id BIGINT DEFAULT NULL COMMENT '当前使用记录ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 使用记录表
CREATE TABLE IF NOT EXISTS disk_usage_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_seq INT DEFAULT NULL COMMENT '显示序号(删除后重新排序)',
    disk_id BIGINT NOT NULL COMMENT '硬盘ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1=出库 3=入库待备份 4=入库已备份',
    out_time DATETIME DEFAULT NULL COMMENT '出库时间',
    in_time DATETIME DEFAULT NULL COMMENT '入库时间',
    storage_content TEXT DEFAULT NULL COMMENT '存储内容说明',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    parent_record_id BIGINT DEFAULT NULL COMMENT '父记录ID(用于记录硬盘使用历史链)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 违规记录表
CREATE TABLE IF NOT EXISTS violation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    disk_id BIGINT DEFAULT NULL COMMENT '硬盘ID',
    record_id BIGINT DEFAULT NULL COMMENT '使用记录ID',
    type VARCHAR(50) NOT NULL COMMENT '违规类型 timeout/reuse/delete_disk_active/delete_record/inbound_invalid_status',
    description VARCHAR(500) DEFAULT NULL COMMENT '违规描述',
    status TINYINT DEFAULT 0 COMMENT '状态 0=pending 1=resolved',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    handled_time DATETIME DEFAULT NULL COMMENT '处理时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 规则配置表
CREATE TABLE IF NOT EXISTS rule_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_key VARCHAR(100) NOT NULL UNIQUE COMMENT '规则键',
    rule_value VARCHAR(255) NOT NULL COMMENT '规则值',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员
INSERT INTO sys_user (username, password, display_name, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ADMIN', 1);

-- 插入角色
INSERT INTO sys_role (name, role_key, description) VALUES
('系统管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('普通用户', 'USER', '普通用户，可查询和操作基本功能');

-- 插入规则配置
INSERT INTO rule_config (rule_key, rule_value, description, status) VALUES
('timeout_days', '7', '硬盘出库超时天数，超时未入库视为超时', 1),
('feishu_app_id', '', '飞书应用 App ID，用于飞书在线表格导出', 1),
('feishu_app_secret', '', '飞书应用 App Secret，用于飞书在线表格导出', 1),
('feishu_spreadsheet_token', '', '飞书在线表格 Token，从表格 URL 中获取', 1),
('auto_idle_on_inbound', '1', '入库时是否自动将硬盘标记为闲置（1=是 0=否）', 1),
('require_storage_content', '1', '出库时是否要求填写存储内容（1=是 0=否）', 1),
('default_role', 'USER', '新注册用户的默认角色（USER 或 ADMIN）', 1);