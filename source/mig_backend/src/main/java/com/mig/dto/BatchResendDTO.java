package com.mig.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 网关日志批量重发（/admin/logs/batch-resend）入参。
 */
@Data
public class BatchResendDTO {

    /**
     * 待重发的网关日志主键集合
     */
    @NotEmpty(message = "请选择要重发的交易")
    private List<Long> ids;
}
