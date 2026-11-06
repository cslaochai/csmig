package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 失败笔数按小时的分布计数（/admin/alerts/summary 的 hourly）。
 */
@Data
public class HourCountVO implements Serializable {

    /**
     * 小时（两位小时文本，取值 00-23）
     */
    private String hour;

    /**
     * 该小时失败笔数
     */
    private Long cnt;
}
