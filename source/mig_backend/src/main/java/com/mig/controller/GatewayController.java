package com.mig.controller;

import com.mig.service.GatewayForwardService;
import org.springframework.web.bind.annotation.*;

/**
 * 前置机统一入口：HIS 所有医保请求都打到这里，按报文类型转发上游。
 *
 * <p>路径与 csmis 对齐：/gateway/{msgType} 透传；/insure/person/query 参保查询透传；
 * /gateway/dayBill 日对账为 GET 无体明文（双侧不进信封）。
 * 国密校验统一在 {@code GatewayForwardService} 完成（院内段验签解密 → 局端段加签加密）。
 */
@RestController
public class GatewayController {

    private final GatewayForwardService forwardService;

    public GatewayController(GatewayForwardService forwardService) {
        this.forwardService = forwardService;
    }

    /**
     * 通用报文转发：/gateway/2201、/gateway/2301、/gateway/2304、/gateway/2305…
     */
    @PostMapping("/gateway/{msgType}")
    public String gateway(@PathVariable String msgType, @RequestBody(required = false) String body) {
        return forwardService.forward(msgType, body);
    }

    /** 参保查询（HIS 的 InsuranceChannelService.checkInsured 走这个路径） */
    @PostMapping("/insure/person/query")
    public String insureQuery(@RequestBody(required = false) String body) {
        return forwardService.forward("insurePersonQuery", body);
    }

    /** 日对账（HIS 拉对方账）：GET 无请求体，透传明文，不进国密信封 */
    @GetMapping("/gateway/dayBill")
    public String dayBill(@RequestParam String billDate) {
        return forwardService.forwardDayBill(billDate);
    }
}
