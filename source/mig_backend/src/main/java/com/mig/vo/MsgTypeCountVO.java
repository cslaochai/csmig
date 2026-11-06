package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 失败笔数按报文类型的分布计数（/admin/alerts/summary 的 byType）。
 */
@Data
public class MsgTypeCountVO implements Serializable {

    /**
     * 报文类型（医保交易编码）
     */
    private String msgType;

    /**
     * 该类型失败笔数
     */
    private Long cnt;
}
