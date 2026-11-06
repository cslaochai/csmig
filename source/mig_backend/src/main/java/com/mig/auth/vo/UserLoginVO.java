package com.mig.auth.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 当前登录用户信息（/auth/info，顶栏用户区渲染）。
 */
@Data
public class UserLoginVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String username;

    private String realName;

    private String currentRole;
}
