package com.mig.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 国密加解密开关状态（/admin/crypto/status 密钥与证书页）。
 */
@Data
public class CryptoStatusVO implements Serializable {

    /**
     * 加解密开关是否开启
     */
    private Boolean enabled;

    /**
     * 加解密算法
     */
    private String algo;

    /**
     * 当前运行模式
     */
    private String mode;

    /**
     * 配置说明
     */
    private String note;
}
