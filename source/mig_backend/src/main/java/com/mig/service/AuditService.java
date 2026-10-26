package com.mig.service;

import com.mig.entity.AuditLog;
import com.mig.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作审计：管理后台关键操作留痕（登录/重发/目录变更/对账导出）。
 * 用户名取自 AuthInterceptor 写入的 request attribute。
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditMapper;

    public void record(HttpServletRequest request, String action, String target, String detail) {
        Object username = request.getAttribute("currentUsername");
        record(request, username == null ? "anonymous" : String.valueOf(username), action, target, detail);
    }

    public void record(HttpServletRequest request, String username, String action, String target, String detail) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setIp(clientIp(request));
        auditMapper.insert(log);
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
