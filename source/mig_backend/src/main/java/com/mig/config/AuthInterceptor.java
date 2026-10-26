package com.mig.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mig.auth.util.JwtUtils;
import com.mig.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录鉴权拦截器：校验 Authorization: Bearer &lt;token&gt;（JWT）。
 * 对齐 cslk HIS 的会话协议；放行路径见 WebConfig。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            if (jwtUtils.validateToken(token)) {
                request.setAttribute("currentUserId", jwtUtils.getUserIdFromToken(token));
                request.setAttribute("currentUsername", jwtUtils.getUsernameFromToken(token));
                request.setAttribute("currentRole", jwtUtils.getCurrentRoleFromToken(token));
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, "未登录或登录已过期，请重新登录")));
        return false;
    }
}
