package com.mig.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 单日对账统计摘要（/admin/recon/summary 对账报表页统计卡）。
 */
@Data
public class ReconSummaryVO implements Serializable {

    /**
     * 对账日期（yyyy-MM-dd 文本）
     */
    private String date;

    /**
     * 当日交易总笔数
     */
    private Long total;

    /**
     * 当日成功笔数
     */
    private Long success;

    /**
     * 当日失败笔数
     */
    private Long fail;

    /**
     * 当日成功率（百分比）
     */
    private Double successRate;

    /**
     * 当日平均处理耗时（毫秒）
     */
    private Long avgCostMs;

    /**
     * 当日按报文类型的三段计数
     */
    private List<MsgTypeStatVO> byType;
}
