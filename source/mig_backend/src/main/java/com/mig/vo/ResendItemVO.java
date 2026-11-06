package com.mig.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

/**
 * 批量重发的单行结果（/admin/logs/batch-resend 结果列表元素）。
 */
@Data
public class ResendItemVO implements Serializable {

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
     * 本笔是否重发成功
     */
    private Boolean ok;

    /**
     * 重发结果说明（失败时为错误信息）
     */
    private String msg;
}
