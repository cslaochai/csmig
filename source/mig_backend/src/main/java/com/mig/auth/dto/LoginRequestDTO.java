package com.mig.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求：用户名 + SM2 加密后的口令密文。
 */
@Data
public class LoginRequestDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 口令密文：前端 sm-crypto 加密后的十六进制（不带 04 前缀，C1C3C2 模式）。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
