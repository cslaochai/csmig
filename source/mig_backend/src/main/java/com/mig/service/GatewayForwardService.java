package com.mig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mig.config.GatewayConfig;
import com.mig.entity.GatewayLog;
import com.mig.mapper.GatewayLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;

/**
 * 前置机核心转发：收HIS明文请求 → （开发期透传/生产期加签加密）→ 转发医保局 → 落日志 → 明文返回HIS。
 *
 * <p>加签加密（SM2/SM4/CA证书）在生产期在此类内加，HIS侧和Controller都不用动。
 */
@Slf4j
public class GatewayForwardService {

    private static final MediaType JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final RestClient upstream;
    private final GatewayLogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GatewayForwardService(RestClient upstream, GatewayLogMapper logMapper,
                                 GatewayConfig.UpstreamProps props) {
        this.upstream = upstream;
        this.logMapper = logMapper;
        log.info("[前置机] 启动，上游平台：{}", props.getBaseUrl());
    }

    /**
     * 通用转发：把HIS发来的body原样POST到上游 /gateway/{msgType}。
     *
     * @param msgType 报文类型（2201/2301/2304/2305…）
     * @param body    HIS请求原文
     * @return 上游响应原文（字符串，原样透传给HIS）
     */
    public String forward(String msgType, String body) {
        String tradeNo = extractTradeNo(body);
        long start = System.currentTimeMillis();
        String response = null;
        int success = 0;
        String errMsg = null;
        try {
            // TODO: 生产期在此处加 SM2签名 + SM4加密（crypto.enabled=true时）
            response = upstream.post()
                    .uri("/gateway/" + msgType)
                    .contentType(JSON_UTF8)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body == null ? "" : body)
                    .retrieve()
                    .body(String.class);
            success = isSuccess(response) ? 1 : 0;
            if (success == 0) {
                errMsg = extractErrMsg(response);
            }
            return response;
        } catch (RestClientException e) {
            errMsg = "上游平台连接失败：" + e.getMessage();
            log.error("[前置机] 转发失败 msgType={} tradeNo={} err={}", msgType, tradeNo, e.getMessage());
            // 返回统一错误结构（HIS侧解析口径与csmis一致）
            return "{\"code\":500,\"message\":\"" + escape(errMsg) + "\",\"data\":null}";
        } finally {
            int costMs = (int) (System.currentTimeMillis() - start);
            saveLog(msgType, tradeNo, success, costMs, body, response, errMsg);
        }
    }

    /** 从请求body里粗提tradeNo（用于日志关联，失败不阻断） */
    private String extractTradeNo(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            String t = node.path("tradeNo").asText(null);
            if (t != null) return t;
            // 2201/2301请求在data段外，有的包在data里
            return node.path("data").path("tradeNo").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** 判断上游返回是否成功（code=200且data.infcode=0） */
    private boolean isSuccess(String response) {
        if (response == null) return false;
        try {
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt(-1);
            if (code != 200) return false;
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) return true; // dayBill等数组结构
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
            row.setMsgType(msgType);
            row.setTradeNo(tradeNo);
            row.setSuccess(success);
            row.setCostMs(costMs);
            // 报文可能很大，截到16k（MEDIUMTEXT上限16M，这里保守截断防日志爆库）
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
