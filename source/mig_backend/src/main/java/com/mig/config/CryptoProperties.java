package com.mig.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 前置机报文安全配置（mip.crypto.*）。
 *
 * <p>前置机是院内/局端两条安全域的边界：
 * <ul>
 *   <li>sm4-key-his：HIS↔前置 方向 SM4 会话密钥（与 HIS 侧一致），院内密钥不出前置；</li>
 *   <li>sm4-key-mss：前置↔医保局 方向 SM4 会话密钥（与医保局端一致），局端密钥不进院内。</li>
 * </ul>
 * 签名：收 HIS 报文验 HIS 证书（csssl 签发）；发医保局用本端证书签名；回执回 HIS 用本端证书签名。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mip.crypto")
public class CryptoProperties {

    /** 国密开关：false=明文透传（开发期）；true=双向验签 + SM4 加密转发 */
    private boolean enabled = false;

    /** 院内段 SM4 会话密钥（32 位十六进制，与 HIS 一致） */
    private String sm4KeyHis = "";

    /** 局端段 SM4 会话密钥（32 位十六进制，与医保局端一致） */
    private String sm4KeyMss = "";

    /** 本端 SM2 签名私钥 PEM（classpath 资源路径） */
    private Resource privateKey;

    /** 本端证书链 PEM（叶子 + SUB_CA，csssl 签发） */
    private Resource certChain;

    /** 信任根：csssl 根证书 PEM */
    private Resource rootCa;

    /** 信任链：csssl SUB_CA 证书 PEM */
    private Resource subCa;

    /** 报文时间窗（秒），超窗按重放拒绝 */
    private int clockSkewSeconds = 300;
}
