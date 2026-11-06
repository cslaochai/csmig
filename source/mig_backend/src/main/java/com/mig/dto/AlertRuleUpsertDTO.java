package com.mig.dto;

import com.mig.common.validation.AllowedValues;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 告警规则新增/修改（/admin/alerts/rules、/admin/alerts/rules/{id}）入参。
 */
@Data
public class AlertRuleUpsertDTO {

    /**
     * 规则类型（FAIL_RATE-失败率 FAIL_COUNT-失败笔数 AVG_COST-平均耗时）
     */
    @NotBlank(message = "规则类型不能为空")
    @AllowedValues(values = {"FAIL_RATE", "FAIL_COUNT", "AVG_COST"}, message = "规则类型不合法（FAIL_RATE-失败率 FAIL_COUNT-失败笔数 AVG_COST-平均耗时）")
    private String ruleType;

    /**
     * 阈值（失败率为百分数，如 10.00 表示 10%）
     */
    @NotNull(message = "阈值不能为空")
    @DecimalMin(value = "0", message = "阈值不能为负数")
    @Digits(integer = 8, fraction = 2, message = "阈值整数位不能超过8位、小数位不能超过2位")
    private BigDecimal threshold;

    /**
     * 启用状态（1-启用 0-停用）
     */
    @NotNull(message = "启用状态不能为空（1-启用 0-停用）")
    @AllowedValues(values = {"0", "1"}, message = "启用状态取值不合法（1-启用 0-停用）")
    private Integer enabled;

    /**
     * 规则说明
     */
    @Size(max = 200, message = "规则说明不能超过200字")
    private String remark;
}
