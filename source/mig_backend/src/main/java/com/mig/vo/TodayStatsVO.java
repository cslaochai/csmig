package com.mig.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 今日交易总览（/admin/stats/today 监控面板统计卡）。
 */
@Data
public class TodayStatsVO implements Serializable {

    /**
     * 今日交易总笔数
     */
    private Long total;

    /**
     * 今日成功笔数
     */
    private Long success;

    /**
     * 今日失败笔数
     */
    private Long fail;

    /**
     * 今日成功率（百分比）
     */
    private Double successRate;

    /**
     * 今日平均处理耗时（毫秒）
     */
    private Long avgCostMs;

    /**
     * 今日按报文类型的三段计数
     */
    private List<MsgTypeStatVO> byType;

    /**
     * 最近交易流水明细
     */
    private List<GatewayLogListVO> recent;
}
