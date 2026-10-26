package com.mig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志：管理后台关键操作留痕（登录/重发/目录变更/对账导出），满足等保审计要求。
 */
@Data
@TableName("mip_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作人（登录用户名） */
    private String username;
    /** 操作：LOGIN/LOGOUT/RESEND/DICT_CREATE/DICT_DELETE/RECON_EXPORT */
    private String action;
    /** 操作对象（日志ID、目录ID等） */
    private String target;
    /** 操作详情 */
    private String detail;
    /** 操作来源IP */
    private String ip;
    private LocalDateTime createdAt;
}
