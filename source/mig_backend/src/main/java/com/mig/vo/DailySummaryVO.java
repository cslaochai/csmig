package com.mig.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 近 N 日结算汇总（/admin/summary/daily 结算汇总页趋势图）。
 */
@Data
public class DailySummaryVO implements Serializable {

    /**
     * 统计天数
     */
    private Integer days;

    /**
     * 日×报文类型聚合行
     */
    private List<DailyMsgTypeStatVO> rows;
}
