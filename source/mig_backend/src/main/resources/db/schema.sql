-- 前置机请求/响应日志：每笔HIS→医保局调用落一条，对账/排查重复扣费/审计用
CREATE TABLE IF NOT EXISTS mip_gateway_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    msg_type     VARCHAR(16)  NOT NULL COMMENT '报文类型（2201/2301/2304/2305/dayBill等）',
    trade_no     VARCHAR(64)  NOT NULL COMMENT '业务幂等号（HIS传的）',
    success      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-失败 1-成功',
    cost_ms      INT          NULL COMMENT '上游耗时毫秒',
    request_body  MEDIUMTEXT   NULL COMMENT 'HIS请求原文（明文JSON）',
    response_body MEDIUMTEXT  NULL COMMENT '医保局响应原文（明文JSON）',
    err_msg      VARCHAR(500) NULL COMMENT '失败原因',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_trade_no (trade_no),
    KEY idx_msg_type_time (msg_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前置机网关日志';

-- 前置机管理用户：登录鉴权（对齐 cslk sys_user 核心字段；初始 admin 账号由 DataInitializer 创建）
CREATE TABLE IF NOT EXISTS mip_sys_user (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    username         VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password         VARCHAR(128) NOT NULL COMMENT 'BCrypt密码哈希',
    real_name        VARCHAR(64)  NOT NULL COMMENT '姓名',
    role_code        VARCHAR(32)  NOT NULL DEFAULT 'ADMIN' COMMENT '角色编码',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1-启用 0-禁用',
    last_login_time  DATETIME     NULL COMMENT '最近登录时间',
    last_login_ip    VARCHAR(64)  NULL COMMENT '最近登录IP',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前置机管理用户';

-- 医保目录缓存：院内目录 ↔ 医保目录匹配关系（药品/诊疗/耗材/体外诊断试剂），开发期本地维护，生产期对接医保局目录下载接口
CREATE TABLE IF NOT EXISTS mip_dict_cache (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    dict_type    VARCHAR(16)  NOT NULL COMMENT '目录类型：DRUG-药品 DIAG-诊疗项目 MAT-医用耗材 IVD-体外诊断试剂',
    his_code     VARCHAR(64)  NOT NULL COMMENT '院内编码',
    his_name     VARCHAR(200) NOT NULL COMMENT '院内名称',
    medi_code    VARCHAR(64)  NULL COMMENT '医保编码',
    medi_name    VARCHAR(200) NULL COMMENT '医保名称',
    match_status TINYINT      NOT NULL DEFAULT 0 COMMENT '匹配状态：0-未匹配 1-已匹配',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (dict_type, his_code),
    KEY idx_type_status (dict_type, match_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医保目录缓存';

-- 操作审计日志：登录/重发/目录变更/对账导出等管理操作留痕，等保审计用
CREATE TABLE IF NOT EXISTS mip_audit_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(64)  NOT NULL COMMENT '操作人',
    action     VARCHAR(32)  NOT NULL COMMENT '操作：LOGIN/LOGOUT/RESEND/DICT_CREATE/DICT_DELETE/RECON_EXPORT',
    target     VARCHAR(64)  NULL COMMENT '操作对象（日志ID/目录ID等）',
    detail     VARCHAR(500) NULL COMMENT '操作详情',
    ip         VARCHAR(64)  NULL COMMENT '操作来源IP',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_action_time (action, created_at),
    KEY idx_username_time (username, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- 告警规则：失败率/超时阈值配置，生产期由定时任务按规则扫描并推送
CREATE TABLE IF NOT EXISTS mip_alert_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    rule_type   VARCHAR(32)  NOT NULL COMMENT '规则类型：FAIL_RATE-失败率 FAIL_COUNT-失败笔数 AVG_COST-平均耗时',
    threshold   DECIMAL(10,2) NOT NULL COMMENT '阈值（失败率为百分数，如10.00表示10%）',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
    remark      VARCHAR(200) NULL COMMENT '规则说明',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则';
