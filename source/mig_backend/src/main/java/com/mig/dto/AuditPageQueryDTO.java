package com.mig.dto;

import com.mig.common.PageParam;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作审计日志分页查询（/admin/audit/page）入参。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditPageQueryDTO extends PageParam {

    /**
     * 操作类型（LOGIN/LOGOUT/RESEND/DICT_CREATE/DICT_DELETE/RECON_EXPORT 等；不传不筛选）
     */
    private String action;

    /**
     * 操作人登录账号（模糊查询）
     */
    private String username;

    /**
     * 查询日期（yyyy-MM-dd；不传或空串均按「不按日筛选」处理）
     */
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "日期格式应为 yyyy-MM-dd")
    private String date;
}
