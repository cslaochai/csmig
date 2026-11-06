package com.mig.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量重发结果（/admin/logs/batch-resend 失败补偿多选重发）。
 */
@Data
public class BatchResendResultVO implements Serializable {

    /**
     * 重发成功笔数
     */
    private Integer ok;

    /**
     * 重发失败笔数
     */
    private Integer fail;

    /**
     * 逐笔重发结果
     */
    private List<ResendItemVO> results;
}
