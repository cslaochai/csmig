package com.mig.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mig.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 前置机管理用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
