package com.mig.support;

import cn.hutool.core.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mig.config.CryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.core.io.Resource;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 医保报文安全信封（CSSSL-SEC-V1）：SM4 加密业务报文 + SM2(SM3withSM2) 签名 + 证书链随报文携带。
 *
 * <p>canonical 待签串 {@code encData|timeStamp|certSerial}；启用数字信封时为
 * {@code keyEnc|encData|timeStamp|certSerial}（keyEnc 参与签名，防止中途换掉会话密钥）。
 * 验签流程：时间窗防重放 → 序列号与信封一致 → 叶子由 SUB_CA 签发、SUB_CA 由 csssl 根签发 →
 * 有效期 → 验签 → 解密。</p>
 *
 * <p><b>加密方式两档并存</b>：
 * <ul>
 *   <li>预共享（院内段 HIS↔前置）：双方各持一把 SM4 密钥，构造时传入 keyHex；</li>
 *   <li>数字信封（前置↔医保局）：构造时传入对端 SM2 公钥，每次转发随机生成 SM4 会话密钥，
 *       用对端公钥 SM2 加密后放 {@code keyEnc}，对端用自己私钥解——跨机构无需共享对称密钥。</li>
 * </ul>
 * 解密时按信封里有没有 {@code keyEnc} 自动选路，旧信封（无 keyEnc）继续按预共享密钥解。</p>
 */
@Slf4j
public class SecMessageCrypto {

    public static final String VERSION = "CSSSL-SEC-V1";

    /** 数字信封用的 SM4 会话密钥长度（16 字节 = 128 位） */
    private static final int SESSION_KEY_BYTES = 16;
    /** SM2 密文格式：与 Hutool / 常见国密实现一致 */
    private static final SM2Engine.Mode SM2_MODE = SM2Engine.Mode.C1C3C2;

    private final CryptoProperties props;
    /** 预共享密钥（数字信封模式下不参与正文加密，仅作为旧信封解密的兜底） */
    private final byte[] sm4Key;
    /** 对端 SM2 公钥；非空 = 本方向改用数字信封 */
    private final PublicKey peerPublicKey;
    private final PrivateKey localPrivateKey;
    private final String localCertSerial;
    private final List<X509Certificate> localChain;
    private final X509Certificate trustRoot;
    private final X509Certificate trustSubCa;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * @param keyHex        当前方向预共享 SM4 会话密钥（可为空——纯数字信封方向不需要）
     * @param peerPublicKey 对端 SM2 公钥；为空则退回预共享密钥模式
     */
    public SecMessageCrypto(CryptoProperties props, String keyHex, Resource peerPublicKey) {
        this.props = props;
        this.sm4Key = (keyHex == null || keyHex.isBlank()) ? null : HexUtil.decodeHex(keyHex);
        this.peerPublicKey = peerPublicKey == null ? null : readPublicKey(peerPublicKey);
        this.localPrivateKey = readPrivateKey(props.getPrivateKey());
        this.localChain = readCertChain(props.getCertChain());
        this.localCertSerial = localChain.get(0).getSerialNumber().toString(16);
        this.trustRoot = readSingleCert(props.getRootCa());
        this.trustSubCa = readSingleCert(props.getSubCa());
    }

    /** 业务报文 → 安全信封 JSON */
    public String buildEnvelope(String businessJson) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("version", VERSION);

        byte[] sessionKey = sm4Key;
        String keyEnc = null;
        if (peerPublicKey != null) {
            // 数字信封：一次一密，密钥只以 SM2 密文形式随报文走
            sessionKey = newSessionKey();
            keyEnc = sm2EncryptSessionKey(sessionKey);
            node.put("keyEnc", keyEnc);
            node.put("keyAlg", "SM2");
        } else if (sessionKey == null) {
            throw new IllegalStateException("既未配置对端 SM2 公钥，也未配置预共享 SM4 密钥，无法构造安全信封");
        }

        String encData = sm4Encrypt(businessJson, sessionKey);
        String timeStamp = Instant.now().toString();
        String canonical = canonical(encData, timeStamp, localCertSerial, keyEnc);
        String signHex = HexUtil.encodeHexStr(sm2Sign(canonical));
        node.put("encData", encData);
        node.put("sign", signHex);
        node.put("certSerial", localCertSerial);
        node.put("timeStamp", timeStamp);
        node.put("certPem", Base64.getEncoder().encodeToString(chainPemBytes()));
        return node.toString();
    }

    /** 安全信封 JSON → 业务报文明文；失败抛 IllegalStateException（携带可读原因） */
    public String unpackEnvelope(String envelopeJson) {
        try {
            JsonNode env = objectMapper.readTree(envelopeJson);
            if (!VERSION.equals(env.path("version").asText())) {
                throw new IllegalStateException("不支持的报文安全版本：" + env.path("version").asText());
            }
            String encData = env.path("encData").asText();
            String signHex = env.path("sign").asText();
            String certSerial = env.path("certSerial").asText();
            String timeStamp = env.path("timeStamp").asText();
            String certPemB64 = env.path("certPem").asText();
            if (encData.isBlank() || signHex.isBlank() || certSerial.isBlank()
                    || timeStamp.isBlank() || certPemB64.isBlank()) {
                throw new IllegalStateException("安全信封字段缺失（encData/sign/certSerial/timeStamp/certPem）");
            }
            Instant ts = Instant.parse(timeStamp);
            long skew = Math.abs(Instant.now().getEpochSecond() - ts.getEpochSecond());
            if (skew > props.getClockSkewSeconds()) {
                throw new IllegalStateException("报文时间戳超出时间窗（" + skew + "s > " + props.getClockSkewSeconds() + "s），疑似重放");
            }
            List<X509Certificate> chain = parseCertChain(Base64.getDecoder().decode(certPemB64));
            X509Certificate leaf = chain.get(0);
            if (!leaf.getSerialNumber().toString(16).equalsIgnoreCase(certSerial)) {
                throw new IllegalStateException("证书序列号与信封不一致");
            }
            leaf.checkValidity();
            trustSubCa.checkValidity();
            trustRoot.checkValidity();
            leaf.verify(trustSubCa.getPublicKey());
            trustSubCa.verify(trustRoot.getPublicKey());
            String keyEnc = env.path("keyEnc").asText(null);
            String canonical = canonical(encData, timeStamp, certSerial, keyEnc);
            if (!sm2Verify(leaf.getPublicKey(), canonical, HexUtil.decodeHex(signHex))) {
                throw new IllegalStateException("SM2 验签失败：报文被篡改或签名值不符");
            }
            // 验签通过后再解密钥，避免用伪造信封消耗本端私钥运算
            byte[] sessionKey;
            if (keyEnc != null && !keyEnc.isBlank()) {
                sessionKey = sm2DecryptSessionKey(keyEnc);
            } else if (sm4Key != null) {
                sessionKey = sm4Key;
            } else {
                throw new IllegalStateException("信封未携带 keyEnc，本方向又未配置预共享 SM4 密钥，无法解密");
            }
            return sm4Decrypt(encData, sessionKey);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("报文安全信封解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 待签串：启用数字信封时把 keyEnc 一起签进去，防止攻击者替换会话密钥密文。
     * 旧信封（无 keyEnc）保持原格式，存量报文与老服务端继续可验。
     */
    private String canonical(String encData, String timeStamp, String certSerial, String keyEnc) {
        String prefix = (keyEnc == null || keyEnc.isBlank()) ? "" : keyEnc + "|";
        return prefix + encData + "|" + timeStamp + "|" + certSerial;
    }

    private byte[] newSessionKey() {
        byte[] key = new byte[SESSION_KEY_BYTES];
        random.nextBytes(key);
        return key;
    }

    private String sm4Encrypt(String plain, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "SM4"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SM4 加密失败：" + e.getMessage(), e);
        }
    }

    private String sm4Decrypt(String b64, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "SM4"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(b64)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("SM4 解密失败（会话密钥不一致或密文被破坏）", e);
        }
    }

    /** 用对端公钥加密会话密钥（SM2 C1C3C2），结果 Base64 */
    private String sm2EncryptSessionKey(byte[] sessionKey) {
        try {
            SM2Engine engine = new SM2Engine(SM2_MODE);
            engine.init(true, new ParametersWithRandom(gmPublicKeyParameters(peerPublicKey), random));
            byte[] out = engine.processBlock(sessionKey, 0, sessionKey.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("SM2 加密会话密钥失败：" + e.getMessage(), e);
        }
    }

    /** 用本端私钥解会话密钥 */
    private byte[] sm2DecryptSessionKey(String keyEncB64) {
        try {
            SM2Engine engine = new SM2Engine(SM2_MODE);
            engine.init(false, gmPrivateKeyParameters(localPrivateKey));
            byte[] in = Base64.getDecoder().decode(keyEncB64);
            return engine.processBlock(in, 0, in.length);
        } catch (Exception e) {
            throw new IllegalStateException("SM2 解会话密钥失败：信封不是给本端私钥的（" + e.getMessage() + "）", e);
        }
    }

    /**
     * 证书里的公钥是 SPKI，曲线 OID 1.2.840.10045.3.1.7 会被默认解析成 NIST P-256；
     * 必须取点坐标后按国密曲线 sm2p256v1 重建参数，否则加解密会用到错误的曲线。
     */
    private ECPublicKeyParameters gmPublicKeyParameters(PublicKey publicKey) {
        ECPublicKey ecPub = (ECPublicKey) publicKey;
        X9ECParameters params = GMNamedCurves.getByName("sm2p256v1");
        ECPoint q = params.getCurve().createPoint(ecPub.getW().getAffineX(), ecPub.getW().getAffineY());
        ECDomainParameters domain = new ECDomainParameters(
                params.getCurve(), params.getG(), params.getN(), params.getH());
        return new ECPublicKeyParameters(q, domain);
    }

    private ECPrivateKeyParameters gmPrivateKeyParameters(PrivateKey privateKey) {
        ECPrivateKey ecPriv = (ECPrivateKey) privateKey;
        X9ECParameters params = GMNamedCurves.getByName("sm2p256v1");
        ECDomainParameters domain = new ECDomainParameters(
                params.getCurve(), params.getG(), params.getN(), params.getH());
        return new ECPrivateKeyParameters(ecPriv.getS(), domain);
    }

    private byte[] sm2Sign(String content) {
        try {
            Signature sig = Signature.getInstance("SM3withSM2", "BC");
            sig.initSign(localPrivateKey);
            sig.update(content.getBytes(StandardCharsets.UTF_8));
            return sig.sign();
        } catch (Exception e) {
            throw new IllegalStateException("SM2 签名失败：" + e.getMessage(), e);
        }
    }

    private boolean sm2Verify(PublicKey publicKey, String content, byte[] signDer) {
        try {
            // 证书公钥 SPKI 的 OID 1.2.840.10045.3.1.7 会被 BC 默认解析为 NIST P-256；
            // 必须取出点坐标后按 GM 曲线（sm2p256v1）构造公钥参数再验签，否则曲线不同必然失败
            java.security.interfaces.ECPublicKey ecPub = (java.security.interfaces.ECPublicKey) publicKey;
            java.security.spec.ECPoint w = ecPub.getW();
            org.bouncycastle.jce.spec.ECParameterSpec gmSpec =
                    org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("sm2p256v1");
            org.bouncycastle.math.ec.ECPoint q = gmSpec.getCurve().createPoint(w.getAffineX(), w.getAffineY());
            org.bouncycastle.crypto.params.ECDomainParameters domain =
                    new org.bouncycastle.crypto.params.ECDomainParameters(gmSpec.getCurve(), gmSpec.getG(), gmSpec.getN(), gmSpec.getH());
            org.bouncycastle.crypto.params.ECPublicKeyParameters pubParams =
                    new org.bouncycastle.crypto.params.ECPublicKeyParameters(q, domain);
            org.bouncycastle.crypto.Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
            signer.init(false, pubParams);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            signer.update(data, 0, data.length);
            return signer.verifySignature(signDer);
        } catch (Exception e) {
            log.warn("[报文安全] SM2 验签异常：{}", e.getMessage());
            return false;
        }
    }

    private byte[] chainPemBytes() {
        StringBuilder sb = new StringBuilder();
        for (X509Certificate cert : localChain) {
            sb.append(PemUtil.certToPem(cert));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private PrivateKey readPrivateKey(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return PemUtil.readPrivateKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("SM2 私钥加载失败：" + resource.getDescription(), e);
        }
    }

    /** 读对端 SM2 公钥：先按 PEM 证书解（取证书里的公钥），失败再按 PKCS#8/SPKI 公钥解 */
    private PublicKey readPublicKey(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String pem = new String(bytes, StandardCharsets.UTF_8);
            if (pem.contains("BEGIN CERTIFICATE")) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new java.io.ByteArrayInputStream(bytes));
                return cert.getPublicKey();
            }
            String base64 = pem.replaceAll("-----BEGIN [^-]+-----", "")
                    .replaceAll("-----END [^-]+-----", "")
                    .replaceAll("\\s", "");
            byte[] spki = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC", "BC").generatePublic(new java.security.spec.X509EncodedKeySpec(spki));
        } catch (Exception e) {
            throw new IllegalStateException("对端 SM2 公钥加载失败：" + resource.getDescription(), e);
        }
    }

    private List<X509Certificate> readCertChain(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return parseCertChain(in.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("证书链加载失败：" + resource.getDescription(), e);
        }
    }

    private X509Certificate readSingleCert(Resource resource) {
        List<X509Certificate> list = readCertChain(resource);
        if (list.isEmpty()) {
            throw new IllegalStateException("信任证书为空：" + resource.getDescription());
        }
        return list.get(0);
    }

    private List<X509Certificate> parseCertChain(byte[] pemBytes) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        List<X509Certificate> out = new ArrayList<>();
        for (java.security.cert.Certificate c : cf.generateCertificates(new java.io.ByteArrayInputStream(pemBytes))) {
            out.add((X509Certificate) c);
        }
        if (out.isEmpty()) {
            throw new IllegalStateException("PEM 中无证书");
        }
        return out;
    }
}
