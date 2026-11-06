package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 医保上游网关配置（/admin/config 系统配置页）。
 */
@Data
public class UpstreamConfigVO implements Serializable {

    /**
     * 上级医保平台接口地址
     */
    private String upstreamUrl;

    /**
     * 连接超时（毫秒）
     */
    private Integer connectTimeoutMs;

    /**
     * 读取超时（毫秒）
     */
    private Integer readTimeoutMs;
}
