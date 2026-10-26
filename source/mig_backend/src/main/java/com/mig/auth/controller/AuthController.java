package com.mig.auth.controller;

import com.mig.auth.dto.LoginRequestDTO;
import com.mig.auth.service.AuthService;
import com.mig.auth.vo.LoginVO;
import com.mig.auth.vo.PublicKeyVO;
import com.mig.auth.vo.UserLoginVO;
import com.mig.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（与 cslk HIS 的 /auth/* 协议对齐）。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 取当前有效的口令加密公钥，供前端把明文口令加密后再上网。
     */
    @GetMapping("/publicKey")
    public Result<PublicKeyVO> publicKey() {
        return Result.success("获取成功", authService.publicKey());
    }

    /**
     * 账号口令登录：校验通过后签发登录令牌。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest request) {
        return Result.success("登录成功", authService.login(loginRequestDTO, request));
    }

    /**
     * 当前登录者身份：姓名、角色编码。
     */
    @GetMapping("/info")
    public Result<UserLoginVO> getUserInfo() {
        return Result.success(authService.currentUserInfo());
    }

    /**
     * 退出登录（无状态 JWT，前端丢弃本地令牌）。
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return Result.success("登出成功", null);
    }
}
