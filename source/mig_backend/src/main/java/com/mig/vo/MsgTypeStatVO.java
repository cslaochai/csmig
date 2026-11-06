package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 按报文类型的总量/成功/失败三段计数（监控面板与对账摘要的类型分布）。
 */
@Data
public class MsgTypeStatVO implements Serializable {

    /**
     * 报文类型（医保交易编码）
     */
    private String msgType;

    /**
     * 该类型交易总笔数
     */
    private Long total;

    /**
     * 该类型成功笔数
     */
    private Long success;

    /**
     * 该类型失败笔数
     */
    private Long fail;
}
