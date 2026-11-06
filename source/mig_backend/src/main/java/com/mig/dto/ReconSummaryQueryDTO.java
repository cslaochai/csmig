package com.mig.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 对账汇总查询（/admin/recon/summary）入参。
 */
@Data
public class ReconSummaryQueryDTO {

    /**
     * 对账日期（yyyy-MM-dd；不传或空串按今日）
     */
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String date;
}
