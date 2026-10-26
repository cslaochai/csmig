package com.mig.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mig.config.GatewayConfig;
import com.mig.common.Result;
import com.mig.entity.AuditLog;
import com.mig.entity.DictCache;
import com.mig.entity.GatewayLog;
import com.mig.mapper.AuditLogMapper;
import com.mig.mapper.DictCacheMapper;
import com.mig.mapper.GatewayLogMapper;
import com.mig.service.AuditService;
import com.mig.service.GatewayForwardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前置机管理后台接口：监控统计、流水查询、手动重发、配置查看、对账导出。
 * （登录鉴权由 AuthInterceptor 统一拦截，本控制器接口均需登录后访问）
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final GatewayLogMapper logMapper;
    private final DictCacheMapper dictMapper;
    private final AuditLogMapper auditMapper;
    private final AuditService auditService;
    private final GatewayForwardService forwardService;
    private final GatewayConfig.UpstreamProps upstreamProps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mip.crypto.enabled:false}")
    private boolean cryptoEnabled;

    // ---------- ① 监控面板：今日统计 ----------

    @GetMapping("/stats/today")
    public Result<Map<String, Object>> todayStats() {
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
        return Result.success(result);
    }

    // ---------- ② 交易流水分页查询 ----------

    @GetMapping("/logs/page")
    public Result<Map<String, Object>> logsPage(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) String msgType,
                                        @RequestParam(required = false) String tradeNo,
                                        @RequestParam(required = false) String date,
                                        @RequestParam(required = false) Integer success) {
        LambdaQueryWrapper<GatewayLog> q = new LambdaQueryWrapper<>();
        if (msgType != null && !msgType.isBlank()) q.eq(GatewayLog::getMsgType, msgType);
        if (tradeNo != null && !tradeNo.isBlank()) q.like(GatewayLog::getTradeNo, tradeNo);
        if (date != null && !date.isBlank()) {
            LocalDateTime ds = LocalDate.parse(date).atStartOfDay();
            q.ge(GatewayLog::getCreatedAt, ds).lt(GatewayLog::getCreatedAt, ds.plusDays(1));
        }
        if (success != null) q.eq(GatewayLog::getSuccess, success);
        q.orderByDesc(GatewayLog::getId);
        Page<GatewayLog> p = logMapper.selectPage(new Page<>(page, size), q);
        Map<String, Object> r = new HashMap<>();
        r.put("total", p.getTotal());
        r.put("list", p.getRecords());
        return Result.success(r);
    }

    // ---------- ③ 手动重发（补偿超时/结果未知交易） ----------

    @PostMapping("/logs/{id}/resend")
    public Result<Object> resend(@PathVariable Long id, HttpServletRequest request) {
        GatewayLog row = logMapper.selectById(id);
        if (row == null) {
            return Result.error(404, "日志不存在");
        }
        log.info("[前置机手动重发] logId={} msgType={} tradeNo={}", id, row.getMsgType(), row.getTradeNo());
        String response = forwardService.forward(row.getMsgType(), row.getRequestBody());
        auditService.record(request, "RESEND", "log-" + id,
                "重发交易 " + row.getMsgType() + "/" + row.getTradeNo());
        return Result.success("已重发", response);
    }

    // ---------- ③b 批量重发（失败补偿多选） ----------

    @PostMapping("/logs/batch-resend")
    public Result<Map<String, Object>> batchResend(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        List<Long> ids = body.get("ids") == null ? List.of() : body.get("ids");
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("请选择要重发的交易");
        }
        int ok = 0, fail = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> item = new HashMap<>();
            GatewayLog row = logMapper.selectById(id);
            item.put("id", id);
            if (row == null) {
                item.put("ok", false);
                item.put("msg", "日志不存在");
                fail++;
                results.add(item);
                continue;
            }
            item.put("msgType", row.getMsgType());
            item.put("tradeNo", row.getTradeNo());
            try {
                forwardService.forward(row.getMsgType(), row.getRequestBody());
                item.put("ok", true);
                ok++;
            } catch (RuntimeException e) {
                item.put("ok", false);
                item.put("msg", e.getMessage());
                fail++;
            }
            results.add(item);
        }
        auditService.record(request, "RESEND", "batch:" + ids.size(),
                "批量重发 " + ok + " 成功 / " + fail + " 失败");
        Map<String, Object> r = new HashMap<>();
        r.put("ok", ok);
        r.put("fail", fail);
        r.put("results", results);
        return Result.success(r);
    }

    // ---------- ④ 配置查看（动态配置后续可扩展） ----------

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> r = new HashMap<>();
        r.put("upstreamUrl", upstreamProps.getBaseUrl());
        r.put("connectTimeoutMs", upstreamProps.getConnectTimeoutMs());
        r.put("readTimeoutMs", upstreamProps.getReadTimeoutMs());
        return Result.success(r);
    }

    // ---------- ⑤ 对账报表导出CSV ----------

    @GetMapping("/recon/export")
    public ResponseEntity<ByteArrayResource> exportCsv(@RequestParam String date,
                                                       @RequestParam(required = false) String msgType,
                                                       @RequestParam(required = false, defaultValue = "false") boolean failOnly,
                                                       HttpServletRequest request) {
        LocalDateTime ds = LocalDate.parse(date).atStartOfDay();
        LambdaQueryWrapper<GatewayLog> q = new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, ds)
                .lt(GatewayLog::getCreatedAt, ds.plusDays(1));
        if (msgType != null && !msgType.isBlank()) q.eq(GatewayLog::getMsgType, msgType);
        if (failOnly) q.eq(GatewayLog::getSuccess, 0);
        List<GatewayLog> list = logMapper.selectList(q.orderByAsc(GatewayLog::getId));
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
        auditService.record(request, "RECON_EXPORT", "recon:" + date,
                "导出对账CSV " + date + " 共" + list.size() + "条" + (failOnly ? "（仅失败）" : ""));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recon-" + date + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(resource);
    }

    // ---------- ⑤b 对账统计摘要（对账报表页统计卡） ----------

    @GetMapping("/recon/summary")
    public Result<Map<String, Object>> reconSummary(@RequestParam(required = false) String date) {
        LocalDateTime ds = date == null || date.isBlank()
                ? LocalDate.now().atStartOfDay()
                : LocalDate.parse(date).atStartOfDay();
        List<GatewayLog> list = logMapper.selectList(new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, ds)
                .lt(GatewayLog::getCreatedAt, ds.plusDays(1)));
        int total = list.size();
        long success = list.stream().filter(l -> l.getSuccess() == 1).count();
        long fail = total - success;
        double avgMs = list.stream().filter(l -> l.getCostMs() != null)
                .mapToInt(GatewayLog::getCostMs).average().orElse(0);
        Map<String, long[]> byType = new LinkedHashMap<>();
        for (GatewayLog l : list) {
            long[] arr = byType.computeIfAbsent(l.getMsgType(), k -> new long[2]);
            arr[0]++;
            if (l.getSuccess() == 1) arr[1]++;
        }
        Map<String, Object> r = new HashMap<>();
        r.put("date", ds.toLocalDate().toString());
        r.put("total", total);
        r.put("success", success);
        r.put("fail", fail);
        r.put("successRate", total == 0 ? 100 : Math.round(success * 1000.0 / total) / 10.0);
        r.put("avgCostMs", Math.round(avgMs));
        r.put("byType", byType);
        return Result.success(r);
    }

    // ---------- ⑩ 操作审计日志查询 ----------

    @GetMapping("/audit/page")
    public Result<Map<String, Object>> auditPage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String action,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String date) {
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<>();
        if (action != null && !action.isBlank()) q.eq(AuditLog::getAction, action);
        if (username != null && !username.isBlank()) q.like(AuditLog::getUsername, username);
        if (date != null && !date.isBlank()) {
            LocalDateTime ds = LocalDate.parse(date).atStartOfDay();
            q.ge(AuditLog::getCreatedAt, ds).lt(AuditLog::getCreatedAt, ds.plusDays(1));
        }
        q.orderByDesc(AuditLog::getId);
        Page<AuditLog> p = auditMapper.selectPage(new Page<>(page, size), q);
        Map<String, Object> r = new HashMap<>();
        r.put("total", p.getTotal());
        r.put("list", p.getRecords());
        return Result.success(r);
    }

    // ---------- ⑥ 异常告警：今日失败总览 ----------

    @GetMapping("/alerts/summary")
    public Result<Map<String, Object>> alertsSummary() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Long todayTotal = logMapper.selectCount(new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, dayStart));
        Long todayFail = logMapper.selectCount(new LambdaQueryWrapper<GatewayLog>()
                .eq(GatewayLog::getSuccess, 0).ge(GatewayLog::getCreatedAt, dayStart));
        // 今日失败按报文类型分布
        List<Map<String, Object>> byType = logMapper.selectMaps(new QueryWrapper<GatewayLog>()
                .select("msg_type AS msgType", "COUNT(*) AS cnt")
                .eq("success", 0).ge("created_at", dayStart)
                .groupBy("msg_type").orderByDesc("cnt"));
        // 最近24小时失败按小时分布
        LocalDateTime hStart = LocalDateTime.now().minusHours(24);
        List<Map<String, Object>> hourly = logMapper.selectMaps(new QueryWrapper<GatewayLog>()
                .select("DATE_FORMAT(created_at, '%H') AS hour", "COUNT(*) AS cnt")
                .eq("success", 0).ge("created_at", hStart)
                .groupBy("hour").orderByAsc("hour"));
        // 最近10条失败明细
        List<GatewayLog> recent = logMapper.selectList(new LambdaQueryWrapper<GatewayLog>()
                .eq(GatewayLog::getSuccess, 0).orderByDesc(GatewayLog::getId).last("LIMIT 10"));
        Map<String, Object> r = new HashMap<>();
        r.put("todayTotal", todayTotal);
        r.put("todayFail", todayFail);
        r.put("failRate", todayTotal == null || todayTotal == 0
                ? 0 : Math.round(todayFail * 1000.0 / todayTotal) / 10.0);
        r.put("byType", byType);
        r.put("hourly", hourly);
        r.put("recent", recent);
        return Result.success(r);
    }

    // ---------- ⑦ 结算汇总：近N日按 日×报文类型 聚合 ----------

    @GetMapping("/summary/daily")
    public Result<Map<String, Object>> summaryDaily(@RequestParam(defaultValue = "7") int days) {
        int d = Math.max(1, Math.min(days, 30));
        LocalDateTime from = LocalDate.now().minusDays(d - 1L).atStartOfDay();
        List<Map<String, Object>> rows = logMapper.selectMaps(new QueryWrapper<GatewayLog>()
                .select("DATE(created_at) AS day", "msg_type AS msgType", "success AS success", "COUNT(*) AS cnt")
                .ge("created_at", from)
                .groupBy("day", "msg_type", "success")
                .orderByAsc("day"));
        // { day: { msgType: [total, success, fail] } }
        Map<String, Map<String, long[]>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String day = String.valueOf(row.get("day"));
            String type = String.valueOf(row.get("msgType"));
            int success = ((Number) row.get("success")).intValue();
            long cnt = ((Number) row.get("cnt")).longValue();
            long[] arr = byDay.computeIfAbsent(day, k -> new HashMap<>())
                    .computeIfAbsent(type, k -> new long[3]);
            arr[0] += cnt;
            if (success == 1) arr[1] += cnt;
            else arr[2] += cnt;
        }
        Map<String, Object> r = new HashMap<>();
        r.put("days", d);
        r.put("byDay", byDay);
        return Result.success(r);
    }

    // ---------- ⑧ 密钥与证书：国密加解密开关状态 ----------

    @GetMapping("/crypto/status")
    public Result<Map<String, Object>> cryptoStatus() {
        Map<String, Object> r = new HashMap<>();
        r.put("enabled", cryptoEnabled);
        r.put("algo", "SM2签名 + SM4加密");
        r.put("mode", cryptoEnabled ? "生产模式（加签加密）" : "开发模式（明文透传）");
        r.put("note", "生产期需配置CA证书、医保局公钥、SM4密钥（见 application.yml 的 mip.crypto）");
        return Result.success(r);
    }

    // ---------- ⑨ 医保目录缓存管理（开发期本地维护，生产期由医保局目录下载同步） ----------

    @GetMapping("/dict/page")
    public Result<Map<String, Object>> dictPage(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) String dictType,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer matchStatus) {
        LambdaQueryWrapper<DictCache> q = new LambdaQueryWrapper<>();
        if (dictType != null && !dictType.isBlank()) q.eq(DictCache::getDictType, dictType);
        if (matchStatus != null) q.eq(DictCache::getMatchStatus, matchStatus);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(DictCache::getHisCode, keyword).or()
                    .like(DictCache::getHisName, keyword).or()
                    .like(DictCache::getMediCode, keyword).or()
                    .like(DictCache::getMediName, keyword));
        }
        q.orderByDesc(DictCache::getUpdatedAt);
        Page<DictCache> p = dictMapper.selectPage(new Page<>(page, size), q);
        Map<String, Object> r = new HashMap<>();
        r.put("total", p.getTotal());
        r.put("list", p.getRecords());
        return Result.success(r);
    }

    @GetMapping("/dict/stats")
    public Result<List<Map<String, Object>>> dictStats() {
        return Result.success(dictMapper.selectMaps(new QueryWrapper<DictCache>()
                .select("dict_type AS dictType", "COUNT(*) AS total",
                        "COALESCE(SUM(match_status = 1), 0) AS matched")
                .groupBy("dict_type")));
    }

    @PostMapping("/dict")
    public Result<Void> dictCreate(@RequestBody DictCache dict, HttpServletRequest request) {
        if (dict.getDictType() == null || dict.getDictType().isBlank()
                || dict.getHisCode() == null || dict.getHisCode().isBlank()
                || dict.getHisName() == null || dict.getHisName().isBlank()) {
            throw new IllegalArgumentException("目录类型、院内编码、院内名称为必填");
        }
        dict.setMatchStatus(dict.getMatchStatus() == null ? 0 : dict.getMatchStatus());
        dictMapper.insert(dict);
        auditService.record(request, "DICT_CREATE", "dict-" + dict.getId(),
                "新增目录缓存 " + dict.getDictType() + "/" + dict.getHisCode() + " " + dict.getHisName());
        return Result.success("新增成功", null);
    }

    @DeleteMapping("/dict/{id}")
    public Result<Void> dictDelete(@PathVariable Long id, HttpServletRequest request) {
        DictCache row = dictMapper.selectById(id);
        dictMapper.deleteById(id);
        auditService.record(request, "DICT_DELETE", "dict-" + id,
                row == null ? "删除目录缓存 #" + id : "删除目录缓存 " + row.getDictType() + "/" + row.getHisCode() + " " + row.getHisName());
        return Result.success("删除成功", null);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
