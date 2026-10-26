package com.mip.controller;

import com.mip.service.GatewayForwardService;
import org.springframework.web.bind.annotation.*;

/**
 * 前置机统一入口：HIS所有医保请求都打到这里，按报文类型转发上游。
 *
 * <p>路径与csmis对齐：/gateway/{msgType} 透传；/insure/person/query 参保查询也透传。
 * 生产期在此处加签名校验（HIS→前置机的双向认证）。
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

    /** 参保查询（HIS的 InsuranceChannelService.checkInsured 走这个路径） */
    @PostMapping("/insure/person/query")
    public String insureQuery(@RequestBody(required = false) String body) {
        return forwardService.forward("insurePersonQuery", body);
    }

    /** 日对账（HIS拉对方账） */
    @GetMapping("/gateway/dayBill")
    public String dayBill(@RequestParam String billDate) {
        // dayBill是GET，HIS侧用queryParam；前置机也GET转发
        // 这里简化：直接调forwardService（但forward是POST，需要单独处理）
        // 实际开发期HIS直连csmis，这个接口先占位
        return forwardService.forward("dayBill", "billDate=" + billDate);
    }
}
