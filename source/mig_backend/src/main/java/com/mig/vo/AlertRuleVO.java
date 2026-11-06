package com.mig.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则行（/admin/alerts/rules 告警规则配置页列表）。
 */
@Data
public class AlertRuleVO implements Serializable {

    /**
     * 规则ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 规则类型（FAIL_RATE-失败率 FAIL_COUNT-失败笔数 AVG_COST-平均耗时）
     */
    private String ruleType;

    /**
     * 触发阈值（失败率类规则为百分数）
     */
    private BigDecimal threshold;

    /**
     * 启用状态（0-停用 1-启用）
     */
    private Integer enabled;

    /**
     * 规则说明
     */
    private String remark;

    /**
     * 规则创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
