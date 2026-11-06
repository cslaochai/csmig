package com.mig.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 今日异常告警总览（/admin/alerts/summary 异常告警页）。
 */
@Data
public class AlertsSummaryVO implements Serializable {

    /**
     * 今日交易总笔数
     */
    private Long todayTotal;

    /**
     * 今日失败笔数
     */
    private Long todayFail;

    /**
     * 今日失败率（百分比）
     */
    private Double failRate;

    /**
     * 失败笔数按报文类型分布
     */
    private List<MsgTypeCountVO> byType;

    /**
     * 失败笔数按小时分布
     */
    private List<HourCountVO> hourly;

    /**
     * 最近失败交易流水明细
     */
    private List<GatewayLogListVO> recent;
}
