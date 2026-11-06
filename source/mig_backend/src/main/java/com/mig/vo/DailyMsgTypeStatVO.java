package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 近 N 日的 日×报文类型 聚合行（/admin/summary/daily 的 rows）。
 */
@Data
public class DailyMsgTypeStatVO implements Serializable {

    /**
     * 日期（yyyy-MM-dd 文本）
     */
    private String day;

    /**
     * 报文类型（医保交易编码）
     */
    private String msgType;

    /**
     * 当日该类型交易总笔数
     */
    private Long total;

    /**
     * 当日该类型成功笔数
     */
    private Long success;

    /**
     * 当日该类型失败笔数
     */
    private Long fail;
}
