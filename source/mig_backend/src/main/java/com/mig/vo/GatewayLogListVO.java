package com.mig.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交易流水列表行（/admin/logs/page 交易流水页，兼作面板与告警页的最近明细）。
 */
@Data
public class GatewayLogListVO implements Serializable {

    /**
     * 日志ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 报文类型（医保交易编码）
     */
    private String msgType;

    /**
     * 交易流水号（院内唯一交易标识）
     */
    private String tradeNo;

    /**
     * 处理结果（0-失败 1-成功）
     */
    private Integer success;

    /**
     * 网关处理耗时（毫秒）
     */
    private Integer costMs;

    /**
     * 请求报文原文
     */
    private String requestBody;

    /**
     * 响应报文原文
     */
    private String responseBody;

    /**
     * 失败错误信息
     */
    private String errMsg;

    /**
     * 流水记录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
