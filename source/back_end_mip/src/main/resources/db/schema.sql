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
