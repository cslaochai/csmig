package com.mig.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 会话配置（yml 的 mig.jwt.* 段，对齐 cslk HIS 的 jwt 配置口径）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mig.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥：环境项，dev 落 dev 文件、pro 由 .env 注入；留空则 token 签发/校验不可用（fail-closed）。
     */
    private String secret = "";

    /**
     * 会话有效期（毫秒），默认 24 小时。
     */
    private long expiration = 86400000;
}
