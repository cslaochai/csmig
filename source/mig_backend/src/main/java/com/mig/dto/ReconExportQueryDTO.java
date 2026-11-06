package com.mig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 对账明细 CSV 导出（/admin/recon/export）入参。
 */
@Data
public class ReconExportQueryDTO {

    /**
     * 对账日期（yyyy-MM-dd，必填）
     */
    @NotBlank(message = "对账日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String date;

    /**
     * 报文类型（2201/2301 等；不传导出全部类型）
     */
    private String msgType;

    /**
     * 是否仅导出失败笔（true-仅失败 false-全部）
     */
    private boolean failOnly = false;
}
