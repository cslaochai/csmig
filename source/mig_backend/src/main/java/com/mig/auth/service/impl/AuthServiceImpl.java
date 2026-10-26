package com.mig.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mig.auth.dto.LoginRequestDTO;
import com.mig.auth.entity.SysUser;
import com.mig.auth.mapper.SysUserMapper;
import com.mig.auth.service.AuthService;
import com.mig.auth.support.PasswordCipherService;
import com.mig.auth.util.JwtUtils;
import com.mig.auth.vo.LoginVO;
import com.mig.auth.vo.PublicKeyVO;
import com.mig.auth.vo.UserLoginVO;
import com.mig.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 认证服务实现（简化自 cslk HIS 的 AuthServiceImpl：
 * SM2 解密口令 + BCrypt 校验 + JWT 会话；单角色、无岗位切换）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final PasswordCipherService passwordCipherService;

    private final JwtUtils jwtUtils;

    private final AuditService auditService;

    @Override
    public PublicKeyVO publicKey() {
        PublicKeyVO vo = new PublicKeyVO();
        vo.setKeyId(passwordCipherService.getKeyId());
        vo.setPublicKey(passwordCipherService.getPublicKeyHex());
        return vo;
    }

    @Override
    public LoginVO login(LoginRequestDTO loginRequestDTO, HttpServletRequest request) {
        String username = loginRequestDTO.getUsername() == null ? "" : loginRequestDTO.getUsername().trim();
        String password;
        try {
            password = passwordCipherService.decrypt(loginRequestDTO.getPassword());
        } catch (RuntimeException e) {
            log.warn("[登录] 口令密文非法：username={}, 原因={}", username, e.getMessage());
            throw e;
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            log.warn("[登录] 用户不存在：username={}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("[登录] 用户已禁用：username={}", username);
            throw new IllegalArgumentException("账号已禁用，请联系管理员");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            log.warn("[登录] 密码错误：username={}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String role = user.getRoleCode() == null || user.getRoleCode().isBlank() ? "ADMIN" : user.getRoleCode();
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role);

        // 记录登录习惯
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .set(SysUser::getLastLoginIp, clientIp(request));
        sysUserMapper.update(updateWrapper);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setCurrentRole(role);
        auditService.record(request, username, "LOGIN", username, "登录成功");
        log.info("[登录] 成功：username={}, role={}", username, role);
        return vo;
    }

    @Override
    public UserLoginVO currentUserInfo() {
        SysUser user = requireLogin();
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setCurrentRole(user.getRoleCode() == null ? "ADMIN" : user.getRoleCode());
        return vo;
    }

    @Override
    public void logout(HttpServletRequest request) {
        Long userId = currentUserId();
        if (userId != null) {
            Object username = request.getAttribute("currentUsername");
            auditService.record(request, username == null ? "anonymous" : String.valueOf(username),
                    "LOGOUT", String.valueOf(userId), "退出登录");
            log.info("[登出] userId={}", userId);
        }
    }

    // ---------- 私有辅助 ----------

    private SysUser requireLogin() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("未登录");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("未登录");
        }
        return user;
    }

    /**
     * 当前登录用户 ID：由 AuthInterceptor 解析 token 后放入 request attribute。
     */
    private Long currentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object v = attrs.getRequest().getAttribute("currentUserId");
        return v instanceof Long ? (Long) v : null;
    }

    /**
     * 获取客户端真实 IP 地址。
     */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }
}
