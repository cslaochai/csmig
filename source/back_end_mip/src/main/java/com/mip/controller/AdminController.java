package com.mip.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mip.config.GatewayConfig;
import com.mip.entity.GatewayLog;
import com.mip.mapper.GatewayLogMapper;
import com.mip.service.GatewayForwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 前置机管理后台接口：监控统计、流水查询、手动重发、配置查看、对账导出。
 * （轻量内网管理，不做登录鉴权——前置机本身在医院内网，只有运维能访问）
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GatewayLogMapper logMapper;
    private final GatewayForwardService forwardService;
    private final GatewayConfig.UpstreamProps upstreamProps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- ① 监控面板：今日统计 ----------

    @GetMapping("/stats/today")
    public Map<String, Object> todayStats() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<GatewayLog> today = logMapper.selectList(
                new LambdaQueryWrapper<GatewayLog>()
                        .ge(GatewayLog::getCreatedAt, dayStart)
                        .lt(GatewayLog::getCreatedAt, dayEnd));
        int total = today.size();
        long success = today.stream().filter(l -> l.getSuccess() == 1).count();
        long fail = total - success;
        double avgMs = today.stream().filter(l -> l.getCostMs() != null)
                .mapToInt(GatewayLog::getCostMs).average().orElse(0);
        // 按报文类型分组
        Map<String, long[]> byType = new HashMap<>();
        for (GatewayLog l : today) {
            String t = l.getMsgType();
            byType.computeIfAbsent(t, k -> new long[2]);
            byType.get(t)[0]++;
            if (l.getSuccess() == 1) byType.get(t)[1]++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("successRate", total == 0 ? 100 : Math.round(success * 1000.0 / total) / 10.0);
        result.put("avgCostMs", Math.round(avgMs));
        result.put("byType", byType);
        // 最近10条（面板异常高亮）
        List<GatewayLog> recent = logMapper.selectList(
                new LambdaQueryWrapper<GatewayLog>()
                        .orderByDesc(GatewayLog::getId).last("LIMIT 10"));
        result.put("recent", recent);
        return result;
    }

    // ---------- ② 交易流水分页查询 ----------

    @GetMapping("/logs/page")
    public Map<String, Object> logsPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) String msgType,
                                        @RequestParam(required = false) String tradeNo,
                                        @RequestParam(required = false) String date) {
        LambdaQueryWrapper<GatewayLog> q = new LambdaQueryWrapper<>();
        if (msgType != null && !msgType.isBlank()) q.eq(GatewayLog::getMsgType, msgType);
        if (tradeNo != null && !tradeNo.isBlank()) q.like(GatewayLog::getTradeNo, tradeNo);
        if (date != null && !date.isBlank()) {
            LocalDateTime ds = LocalDate.parse(date).atStartOfDay();
            q.ge(GatewayLog::getCreatedAt, ds).lt(GatewayLog::getCreatedAt, ds.plusDays(1));
        }
        q.orderByDesc(GatewayLog::getId);
        Page<GatewayLog> p = logMapper.selectPage(new Page<>(page, size), q);
        Map<String, Object> r = new HashMap<>();
        r.put("total", p.getTotal());
        r.put("list", p.getRecords());
        return r;
    }

    // ---------- ③ 手动重发（补偿超时/结果未知交易） ----------

    @PostMapping("/logs/{id}/resend")
    public Map<String, Object> resend(@PathVariable Long id) {
        GatewayLog row = logMapper.selectById(id);
        if (row == null) {
            return Map.of("code", 404, "message", "日志不存在");
        }
        log.info("[前置机手动重发] logId={} msgType={} tradeNo={}", id, row.getMsgType(), row.getTradeNo());
        String response = forwardService.forward(row.getMsgType(), row.getRequestBody());
        return Map.of("code", 200, "message", "已重发", "data", response);
    }

    // ---------- ④ 配置查看（动态配置后续可扩展） ----------

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> r = new HashMap<>();
        r.put("upstreamUrl", upstreamProps.getBaseUrl());
        r.put("connectTimeoutMs", upstreamProps.getConnectTimeoutMs());
        r.put("readTimeoutMs", upstreamProps.getReadTimeoutMs());
        return r;
    }

    // ---------- ⑤ 对账报表导出CSV ----------

    @GetMapping("/recon/export")
    public ResponseEntity<ByteArrayResource> exportCsv(@RequestParam String date) {
        LocalDateTime ds = LocalDate.parse(date).atStartOfDay();
        List<GatewayLog> list = logMapper.selectList(
                new LambdaQueryWrapper<GatewayLog>()
                        .ge(GatewayLog::getCreatedAt, ds)
                        .lt(GatewayLog::getCreatedAt, ds.plusDays(1))
                        .orderByAsc(GatewayLog::getId));
        StringBuilder sb = new StringBuilder();
        sb.append("ID,报文类型,交易号,成功,耗时(ms),错误信息,时间\n");
        for (GatewayLog l : list) {
            sb.append(l.getId()).append(',')
              .append(l.getMsgType()).append(',')
              .append(escapeCsv(l.getTradeNo())).append(',')
              .append(l.getSuccess() == 1 ? "成功" : "失败").append(',')
              .append(l.getCostMs() == null ? "" : l.getCostMs()).append(',')
              .append(escapeCsv(l.getErrMsg())).append(',')
              .append(l.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
              .append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recon-" + date + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(resource);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
