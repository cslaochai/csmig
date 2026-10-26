package com.mig.auth.vo;

import lombok.Data;

/**
 * 登录口令加密公钥（前端 sm-crypto 加密入参）。
 */
@Data
public class PublicKeyVO {

    /**
     * 公钥指纹（sha256 前 16 位），前端判断公钥是否轮换。
     */
    private String keyId;

    /**
     * SM2 未压缩公钥点十六进制（04 前缀 + X + Y）。
     */
    private String publicKey;
}
