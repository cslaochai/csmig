package com.mig.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 每日交易汇总查询（/admin/summary/daily）入参。
 */
@Data
public class DailySummaryQueryDTO {

    /**
     * 统计天数（1~30，默认 7）
     */
    @Min(value = 1, message = "统计天数不能小于1")
    @Max(value = 30, message = "统计天数不能超过30")
    private Integer days = 7;
}
