package com.mig.auth.service;

import com.mig.auth.dto.LoginRequestDTO;
import com.mig.auth.vo.LoginVO;
import com.mig.auth.vo.PublicKeyVO;
import com.mig.auth.vo.UserLoginVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务：登录、取公钥、当前用户信息、登出。
 */
public interface AuthService {

    /** 取登录口令加密公钥 */
    PublicKeyVO publicKey();

    /** 账号口令登录，签发 JWT */
    LoginVO login(LoginRequestDTO loginRequestDTO, HttpServletRequest request);

    /** 当前登录者信息（从请求上下文取 token） */
    UserLoginVO currentUserInfo();

    /** 登出（无状态 JWT，仅留记录） */
    void logout(HttpServletRequest request);
}
