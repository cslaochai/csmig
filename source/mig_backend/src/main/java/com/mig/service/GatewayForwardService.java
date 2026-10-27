package com.mig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mig.config.CryptoProperties;
import com.mig.config.GatewayConfig;
import com.mig.entity.GatewayLog;
import com.mig.mapper.GatewayLogMapper;
import com.mig.support.SecMessageCrypto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;

/**
 * 前置机核心转发：收 HIS 安全信封 → 验签解密（院内段）→ 业务报文 → 加签加密（局端段）→ 转发医保局 →
 * 回执验签解密 → 加签加密（院内段）→ 返回 HIS。
 *
 * <p>crypto.enabled=true 时启用国密双段（HIS↔前置、前置↔医保局 各一把 SM4 密钥，隔离内外）；
 * false（开发期）保持原样透传。日志落原始信封（密文）与解密后 tradeNo，便于排障又不在库中留存明文报文。
 */
@Slf4j
public class GatewayForwardService {

    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    private static final String MSG_INSURE_QUERY = "insurePersonQuery";
    private static final String MSG_DAY_BILL = "dayBill";

    private final RestClient upstream;
    private final GatewayLogMapper logMapper;
    private final CryptoProperties cryptoProps;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile SecMessageCrypto hisSide;
    private volatile SecMessageCrypto mssSide;

    public GatewayForwardService(RestClient upstream, GatewayLogMapper logMapper,
                                 GatewayConfig.UpstreamProps props, CryptoProperties cryptoProps) {
        this.upstream = upstream;
        this.logMapper = logMapper;
        this.cryptoProps = cryptoProps;
        log.info("[前置机] 启动，上游平台：{}，报文国密：{}",
                props.getBaseUrl(), cryptoProps.isEnabled() ? "SM2签名+SM4加密（双段隔离）" : "明文透传（开发期）");
    }

    /** 收 HIS 安全信封 → 转发 → 回 HIS 安全信封 */
    public String forward(String msgType, String body) {
        String rawIn = body == null ? "" : body;
        String tradeNo = null;
        long start = System.currentTimeMillis();
        String response = null;
        int success = 0;
        String errMsg = null;
        String bizOut = null;
        try {
            String business;
            if (cryptoProps.isEnabled() && !MSG_DAY_BILL.equals(msgType)) {
                // 1. HIS 方向：验签 + 解密
                business = hisSide().unpackEnvelope(rawIn);
                tradeNo = extractTradeNo(business);
                // 2. 转发上游（医保局方向：加签 + 加密）
                String outbound = mssSide().buildEnvelope(business);
                response = upstream.post()
                        .uri(upstreamPath(msgType))
                        .contentType(JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(outbound)
                        .retrieve()
                        .body(String.class);
                // 3. 回执：医保局方向验签解密 → 业务回执
                String bizReceipt = mssSide().unpackEnvelope(response);
                success = isSuccess(bizReceipt) ? 1 : 0;
                if (success == 0) {
                    errMsg = extractErrMsg(bizReceipt);
                }
                // 4. 回 HIS：加签 + 加密（院内段密钥）
                bizOut = hisSide().buildEnvelope(bizReceipt);
            } else {
                // 开发期透传 / dayBill GET 无业务体（对账走专线，生产加 TLS）
                response = upstream.post()
                        .uri(upstreamPath(msgType))
                        .contentType(JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(rawIn)
                        .retrieve()
                        .body(String.class);
                tradeNo = extractTradeNo(rawIn);
                success = isSuccess(response) ? 1 : 0;
                if (success == 0) {
                    errMsg = extractErrMsg(response);
                }
                bizOut = response;
            }
            return bizOut;
        } catch (RestClientException e) {
            errMsg = "上游平台连接失败：" + e.getMessage();
            log.error("[前置机] 转发失败 msgType={} tradeNo={} err={}", msgType, tradeNo, e.getMessage());
            bizOut = errorEnvelopeOrPlain("{\"code\":500,\"message\":\"" + escape(errMsg) + "\",\"data\":null}");
        } catch (Exception e) {
            errMsg = "报文安全处理失败：" + e.getMessage();
            log.error("[前置机] 转发异常 msgType={} tradeNo={} err={}", msgType, tradeNo, e.getMessage());
            bizOut = errorEnvelopeOrPlain("{\"code\":500,\"message\":\"" + escape(errMsg) + "\",\"data\":null}");
        } finally {
            int costMs = (int) (System.currentTimeMillis() - start);
            saveLog(msgType, tradeNo, success, costMs, rawIn, response, errMsg);
        }
        return bizOut;
    }

    /** 医保局端路径：参保查询走 /insure/person/query（csmis 实际路由），其余走 /gateway/{msgType} */
    private String upstreamPath(String msgType) {
        if (MSG_INSURE_QUERY.equals(msgType)) {
            return "/insure/person/query";
        }
        return "/gateway/" + msgType;
    }

    /** 失败回执：国密开启时同样封装成安全信封返回（HIS 侧解包拿原因） */
    private String errorEnvelopeOrPlain(String errorJson) {
        if (cryptoProps.isEnabled()) {
            try {
                return hisSide().buildEnvelope(errorJson);
            } catch (Exception e) {
                log.warn("[前置机] 失败回执封装异常，回明文：{}", e.getMessage());
            }
        }
        return errorJson;
    }

    private SecMessageCrypto hisSide() {
        if (hisSide == null) {
            synchronized (this) {
                if (hisSide == null) {
                    hisSide = new SecMessageCrypto(cryptoProps, cryptoProps.getSm4KeyHis());
                    log.info("[前置机] 院内段安全通道就绪（HIS↔前置）");
                }
            }
        }
        return hisSide;
    }

    private SecMessageCrypto mssSide() {
        if (mssSide == null) {
            synchronized (this) {
                if (mssSide == null) {
                    mssSide = new SecMessageCrypto(cryptoProps, cryptoProps.getSm4KeyMss());
                    log.info("[前置机] 局端段安全通道就绪（前置↔医保局）");
                }
            }
        }
        return mssSide;
    }

    /** 从业务报文里粗提 tradeNo（用于日志关联，失败不阻断） */
    private String extractTradeNo(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            String t = node.path("tradeNo").asText(null);
            if (t != null) return t;
            return node.path("data").path("tradeNo").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** 判断业务回执是否成功（code=200且data.infcode=0） */
    private boolean isSuccess(String response) {
        if (response == null) return false;
        try {
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt(-1);
            if (code != 200) return false;
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) return true;
            return data.path("infcode").asInt(0) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractErrMsg(String response) {
        if (response == null) return null;
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            if (data.has("errMsg")) return data.path("errMsg").asText();
            return root.path("message").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveLog(String msgType, String tradeNo, int success, int costMs,
                         String requestBody, String responseBody, String errMsg) {
        try {
            GatewayLog row = new GatewayLog();
            row.setMsgType(truncate(msgType, 32));
            row.setTradeNo(tradeNo == null || tradeNo.isEmpty() ? "-" : truncate(tradeNo, 64));
            row.setSuccess(success);
            row.setCostMs(costMs);
            // 国密开启时正文为安全信封（密文），不落明文业务报文；截断防爆库
            row.setRequestBody(truncate(requestBody, 16384));
            row.setResponseBody(truncate(responseBody, 16384));
            row.setErrMsg(errMsg);
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("[前置机] 日志落库失败 msgType={} tradeNo={} err={}", msgType, tradeNo, e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...[truncated]";
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "'").replace("\\", "\\\\").replace("\n", " ");
    }
}
