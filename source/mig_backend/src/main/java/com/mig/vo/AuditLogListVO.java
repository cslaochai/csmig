package com.mig.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志列表行（/admin/audit/page 审计日志页）。
 */
@Data
public class AuditLogListVO implements Serializable {

    /**
     * 日志ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 操作人（登录用户名）
     */
    private String username;

    /**
     * 操作类型（LOGIN/LOGOUT/RESEND/RECON_EXPORT/DICT_CREATE/DICT_UPDATE/DICT_DELETE/DICT_DOWNLOAD/ALERT_RULE）
     */
    private String action;

    /**
     * 操作对象
     */
    private String target;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * 操作来源IP
     */
    private String ip;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
