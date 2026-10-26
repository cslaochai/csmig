package com.mig.auth.vo;

import lombok.Data;

/**
 * 登录成功返回体：token + 身份信息（前端存入 localStorage）。
 */
@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String realName;

    private String currentRole;
}
