package com.mig.dto;

import com.mig.common.PageParam;
import com.mig.common.validation.AllowedValues;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网关日志分页查询（/admin/logs/page）入参。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayLogPageQueryDTO extends PageParam {

    /**
     * 报文类型（2201/2301/2304/2305/dayBill 等；不传不筛选）
     */
    private String msgType;

    /**
     * 业务幂等号（HIS 传的，模糊查询）
     */
    private String tradeNo;

    /**
     * 查询日期（yyyy-MM-dd；不传或空串均按「不按日筛选」处理）
     */
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String date;

    /**
     * 成功状态（0-失败 1-成功）
     */
    @AllowedValues(values = {"0", "1"}, message = "成功状态取值不合法（0-失败 1-成功）")
    private Integer success;
}
