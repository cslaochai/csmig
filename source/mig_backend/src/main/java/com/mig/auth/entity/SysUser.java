package com.mig.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前置机管理用户（登录鉴权用，对齐 cslk sys_user 的核心字段）。
 */
@Data
@TableName("mip_sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** BCrypt 密码哈希 */
    private String password;

    private String realName;

    private String roleCode;

    /** 1-启用 0-禁用 */
    private Integer status;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createdAt;
}
