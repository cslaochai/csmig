package com.mig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则：失败率/失败笔数/平均耗时阈值，生产期由定时任务按规则扫描并推送。
 */
@Data
@TableName("mip_alert_rule")
public class AlertRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 规则类型：FAIL_RATE-失败率 FAIL_COUNT-失败笔数 AVG_COST-平均耗时 */
    private String ruleType;
    /** 阈值（失败率为百分数，如 10.00 表示 10%） */
    private BigDecimal threshold;
    /** 1-启用 0-停用 */
    private Integer enabled;
    /** 规则说明 */
    private String remark;
    private LocalDateTime createdAt;
}
