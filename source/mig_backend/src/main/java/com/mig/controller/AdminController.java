package com.mig.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mig.common.PageParam;
import com.mig.common.PageResult;
import com.mig.common.Result;
import com.mig.config.CryptoProperties;
import com.mig.config.GatewayConfig;
import com.mig.dto.AlertRuleUpsertDTO;
import com.mig.dto.AuditPageQueryDTO;
import com.mig.dto.BatchResendDTO;
import com.mig.dto.DailySummaryQueryDTO;
import com.mig.dto.DictDownloadQueryDTO;
import com.mig.dto.DictPageQueryDTO;
import com.mig.dto.DictUpsertDTO;
import com.mig.dto.GatewayLogPageQueryDTO;
import com.mig.dto.ReconExportQueryDTO;
import com.mig.dto.ReconSummaryQueryDTO;
import com.mig.entity.AlertRule;
import com.mig.entity.AuditLog;
import com.mig.entity.DictCache;
import com.mig.entity.GatewayLog;
import com.mig.mapper.AlertRuleMapper;
import com.mig.mapper.AuditLogMapper;
import com.mig.mapper.DictCacheMapper;
import com.mig.mapper.GatewayLogMapper;
import com.mig.service.AuditService;
import com.mig.service.GatewayForwardService;
import com.mig.vo.AlertRuleVO;
import com.mig.vo.AlertsSummaryVO;
import com.mig.vo.AuditLogListVO;
import com.mig.vo.BatchResendResultVO;
import com.mig.vo.CryptoStatusVO;
import com.mig.vo.DailySummaryVO;
import com.mig.vo.DictCacheVO;
import com.mig.vo.DictDownloadResultVO;
import com.mig.vo.DictTypeStatVO;
import com.mig.vo.GatewayLogListVO;
import com.mig.vo.MsgTypeStatVO;
import com.mig.vo.ReconSummaryVO;
import com.mig.vo.ResendItemVO;
import com.mig.vo.TodayStatsVO;
import com.mig.vo.UpstreamConfigVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private static final int RECENT_LIMIT = 10;

    private final GatewayLogMapper logMapper;
    private final DictCacheMapper dictMapper;
    private final AuditLogMapper auditMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final AuditService auditService;
    private final GatewayForwardService forwardService;
    private final GatewayConfig.UpstreamProps upstreamProps;
    private final CryptoProperties cryptoProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------- ① 监控面板：今日统计 ----------

    @GetMapping("/stats/today")
    public Result<TodayStatsVO> todayStats() {
        List<GatewayLog> today = selectToday();
        CountStat count = countOf(today);
        TodayStatsVO vo = new TodayStatsVO();
        vo.setTotal(count.total());
        vo.setSuccess(count.success());
        vo.setFail(count.fail());
        vo.setSuccessRate(count.successRate());
        vo.setAvgCostMs(count.avgCostMs());
        vo.setByType(groupByMsgType(today));
        vo.setRecent(selectRecent().stream().map(AdminController::toLogVO).toList());
        return Result.success(vo);
    }

    // ---------- ② 交易流水分页查询 ----------

    @GetMapping("/logs/page")
    public Result<PageResult<GatewayLogListVO>> logsPage(@Valid GatewayLogPageQueryDTO query) {
        LambdaQueryWrapper<GatewayLog> q = new LambdaQueryWrapper<>();
        if (query.getMsgType() != null && !query.getMsgType().isBlank()) {
            q.eq(GatewayLog::getMsgType, query.getMsgType());
        }
        if (query.getTradeNo() != null && !query.getTradeNo().isBlank()) {
            q.like(GatewayLog::getTradeNo, query.getTradeNo());
        }
        if (query.getDate() != null && !query.getDate().isBlank()) {
            LocalDateTime ds = LocalDate.parse(query.getDate()).atStartOfDay();
            q.ge(GatewayLog::getCreatedAt, ds).lt(GatewayLog::getCreatedAt, ds.plusDays(1));
        }
        if (query.getSuccess() != null) {
            q.eq(GatewayLog::getSuccess, query.getSuccess());
        }
        q.orderByDesc(GatewayLog::getId);
        return Result.success(pageOfVO(logMapper.selectPage(toPage(query), q), AdminController::toLogVO));
    }

    // ---------- ③ 手动重发（补偿超时/结果未知交易） ----------

    @PostMapping("/logs/{id}/resend")
    public Result<String> resend(@PathVariable Long id, HttpServletRequest request) {
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
    public Result<BatchResendResultVO> batchResend(@RequestBody @Valid BatchResendDTO dto, HttpServletRequest request) {
        int ok = 0, fail = 0;
        List<ResendItemVO> results = new ArrayList<>();
        for (Long id : dto.getIds()) {
            ResendItemVO item = new ResendItemVO();
            item.setId(id);
            GatewayLog row = logMapper.selectById(id);
            if (row == null) {
                item.setOk(false);
                item.setMsg("日志不存在");
                fail++;
                results.add(item);
                continue;
            }
            item.setMsgType(row.getMsgType());
            item.setTradeNo(row.getTradeNo());
            try {
                forwardService.forward(row.getMsgType(), row.getRequestBody());
                item.setOk(true);
                ok++;
            } catch (RuntimeException e) {
                item.setOk(false);
                item.setMsg(e.getMessage());
                fail++;
            }
            results.add(item);
        }
        auditService.record(request, "RESEND", "batch:" + dto.getIds().size(),
                "批量重发 " + ok + " 成功 / " + fail + " 失败");
        BatchResendResultVO vo = new BatchResendResultVO();
        vo.setOk(ok);
        vo.setFail(fail);
        vo.setResults(results);
        return Result.success(vo);
    }

    // ---------- ④ 配置查看（动态配置后续可扩展） ----------

    @GetMapping("/config")
    public Result<UpstreamConfigVO> config() {
        UpstreamConfigVO vo = new UpstreamConfigVO();
        vo.setUpstreamUrl(upstreamProps.getBaseUrl());
        vo.setConnectTimeoutMs(upstreamProps.getConnectTimeoutMs());
        vo.setReadTimeoutMs(upstreamProps.getReadTimeoutMs());
        return Result.success(vo);
    }

    // ---------- ⑤ 对账报表导出CSV ----------

    @GetMapping("/recon/export")
    public ResponseEntity<ByteArrayResource> exportCsv(@Valid ReconExportQueryDTO query, HttpServletRequest request) {
        LocalDateTime ds = LocalDate.parse(query.getDate()).atStartOfDay();
        LambdaQueryWrapper<GatewayLog> q = new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, ds)
                .lt(GatewayLog::getCreatedAt, ds.plusDays(1));
        if (query.getMsgType() != null && !query.getMsgType().isBlank()) {
            q.eq(GatewayLog::getMsgType, query.getMsgType());
        }
        if (query.isFailOnly()) {
            q.eq(GatewayLog::getSuccess, 0);
        }
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
        auditService.record(request, "RECON_EXPORT", "recon:" + query.getDate(),
                "导出对账CSV " + query.getDate() + " 共" + list.size() + "条" + (query.isFailOnly() ? "（仅失败）" : ""));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recon-" + query.getDate() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(resource);
    }

    // ---------- ⑤b 对账统计摘要（对账报表页统计卡） ----------

    @GetMapping("/recon/summary")
    public Result<ReconSummaryVO> reconSummary(@Valid ReconSummaryQueryDTO query) {
        LocalDateTime ds = query.getDate() == null || query.getDate().isBlank()
                ? LocalDate.now().atStartOfDay()
                : LocalDate.parse(query.getDate()).atStartOfDay();
        List<GatewayLog> list = logMapper.selectList(new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, ds)
                .lt(GatewayLog::getCreatedAt, ds.plusDays(1)));
        ReconSummaryVO vo = new ReconSummaryVO();
        CountStat count = countOf(list);
        vo.setDate(ds.toLocalDate().toString());
        vo.setTotal(count.total());
        vo.setSuccess(count.success());
        vo.setFail(count.fail());
        vo.setSuccessRate(count.successRate());
        vo.setAvgCostMs(count.avgCostMs());
        vo.setByType(groupByMsgType(list));
        return Result.success(vo);
    }

    // ---------- ⑩ 操作审计日志查询 ----------

    @GetMapping("/audit/page")
    public Result<PageResult<AuditLogListVO>> auditPage(@Valid AuditPageQueryDTO query) {
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<>();
        if (query.getAction() != null && !query.getAction().isBlank()) {
            q.eq(AuditLog::getAction, query.getAction());
        }
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            q.like(AuditLog::getUsername, query.getUsername());
        }
        if (query.getDate() != null && !query.getDate().isBlank()) {
            LocalDateTime ds = LocalDate.parse(query.getDate()).atStartOfDay();
            q.ge(AuditLog::getCreatedAt, ds).lt(AuditLog::getCreatedAt, ds.plusDays(1));
        }
        q.orderByDesc(AuditLog::getId);
        return Result.success(pageOfVO(auditMapper.selectPage(toPage(query), q), AdminController::toAuditVO));
    }

    // ---------- ⑥ 异常告警：今日失败总览 ----------

    @GetMapping("/alerts/summary")
    public Result<AlertsSummaryVO> alertsSummary() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Long todayTotal = logMapper.selectCount(new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, dayStart));
        Long todayFail = logMapper.selectCount(new LambdaQueryWrapper<GatewayLog>()
                .eq(GatewayLog::getSuccess, 0).ge(GatewayLog::getCreatedAt, dayStart));
        AlertsSummaryVO vo = new AlertsSummaryVO();
        vo.setTodayTotal(todayTotal);
        vo.setTodayFail(todayFail);
        vo.setFailRate(todayTotal == null || todayTotal == 0
                ? 0 : Math.round(todayFail * 1000.0 / todayTotal) / 10.0);
        vo.setByType(logMapper.selectFailCountByType(dayStart));
        vo.setHourly(logMapper.selectFailCountByHour(LocalDateTime.now().minusHours(24)));
        vo.setRecent(logMapper.selectList(new LambdaQueryWrapper<GatewayLog>()
                        .eq(GatewayLog::getSuccess, 0).orderByDesc(GatewayLog::getId).last("LIMIT " + RECENT_LIMIT))
                .stream().map(AdminController::toLogVO).toList());
        return Result.success(vo);
    }

    // ---------- ⑦ 结算汇总：近N日按 日×报文类型 聚合 ----------

    @GetMapping("/summary/daily")
    public Result<DailySummaryVO> summaryDaily(@Valid DailySummaryQueryDTO query) {
        int days = query.getDays();
        DailySummaryVO vo = new DailySummaryVO();
        vo.setDays(days);
        vo.setRows(logMapper.selectDailyTypeStats(LocalDate.now().minusDays(days - 1L).atStartOfDay()));
        return Result.success(vo);
    }

    // ---------- ⑧ 密钥与证书：国密加解密开关状态 ----------

    @GetMapping("/crypto/status")
    public Result<CryptoStatusVO> cryptoStatus() {
        CryptoStatusVO vo = new CryptoStatusVO();
        vo.setEnabled(cryptoProperties.isEnabled());
        vo.setAlgo("SM2签名 + SM4加密");
        vo.setMode(cryptoProperties.isEnabled() ? "生产模式（加签加密）" : "开发模式（明文透传）");
        vo.setNote("生产期需配置CA证书、医保局公钥、SM4密钥（见 application.yml 的 mig.crypto）");
        return Result.success(vo);
    }

    // ---------- ⑨ 医保目录缓存管理（开发期本地维护，生产期由医保局目录下载同步） ----------

    @GetMapping("/dict/page")
    public Result<PageResult<DictCacheVO>> dictPage(@Valid DictPageQueryDTO query) {
        LambdaQueryWrapper<DictCache> q = new LambdaQueryWrapper<>();
        if (query.getDictType() != null && !query.getDictType().isBlank()) {
            q.eq(DictCache::getDictType, query.getDictType());
        }
        if (query.getMatchStatus() != null) {
            q.eq(DictCache::getMatchStatus, query.getMatchStatus());
        }
        String keyword = query.getKeyword();
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(DictCache::getHisCode, keyword).or()
                    .like(DictCache::getHisName, keyword).or()
                    .like(DictCache::getMediCode, keyword).or()
                    .like(DictCache::getMediName, keyword));
        }
        q.orderByDesc(DictCache::getUpdatedAt);
        return Result.success(pageOfVO(dictMapper.selectPage(toPage(query), q), AdminController::toDictVO));
    }

    @GetMapping("/dict/stats")
    public Result<List<DictTypeStatVO>> dictStats() {
        return Result.success(dictMapper.selectTypeStats());
    }

    @PostMapping("/dict")
    public Result<Void> dictCreate(@RequestBody @Valid DictUpsertDTO dto, HttpServletRequest request) {
        DictCache dict = new DictCache();
        dict.setDictType(dto.getDictType());
        dict.setHisCode(dto.getHisCode());
        dict.setHisName(dto.getHisName());
        dict.setMediCode(dto.getMediCode());
        dict.setMediName(dto.getMediName());
        dict.setMatchStatus(dto.getMatchStatus());
        dictMapper.insert(dict);
        auditService.record(request, "DICT_CREATE", "dict-" + dict.getId(),
                "新增目录缓存 " + dict.getDictType() + "/" + dict.getHisCode() + " " + dict.getHisName());
        return Result.success("新增成功", null);
    }

    @PutMapping("/dict/{id}")
    public Result<Void> dictUpdate(@PathVariable Long id, @RequestBody @Valid DictUpsertDTO dto, HttpServletRequest request) {
        DictCache row = dictMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("目录不存在");
        }
        row.setDictType(dto.getDictType());
        row.setHisCode(dto.getHisCode());
        row.setHisName(dto.getHisName());
        row.setMediCode(dto.getMediCode());
        row.setMediName(dto.getMediName());
        // 未传匹配状态视为不修改（页面编辑框恒有值，此处兜住只改部分字段的调用方）
        if (dto.getMatchStatus() != null) {
            row.setMatchStatus(dto.getMatchStatus());
        }
        dictMapper.updateById(row);
        auditService.record(request, "DICT_UPDATE", "dict-" + id,
                "修改目录缓存 " + row.getDictType() + "/" + row.getHisCode() + " " + row.getHisName());
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/dict/{id}")
    public Result<Void> dictDelete(@PathVariable Long id, HttpServletRequest request) {
        DictCache row = dictMapper.selectById(id);
        dictMapper.deleteById(id);
        auditService.record(request, "DICT_DELETE", "dict-" + id,
                row == null ? "删除目录缓存 #" + id : "删除目录缓存 " + row.getDictType() + "/" + row.getHisCode() + " " + row.getHisName());
        return Result.success("删除成功", null);
    }

    // ---------- ⑪ 告警规则配置 ----------

    @GetMapping("/alerts/rules")
    public Result<List<AlertRuleVO>> alertRules() {
        return Result.success(alertRuleMapper.selectList(
                        new LambdaQueryWrapper<AlertRule>().orderByAsc(AlertRule::getId))
                .stream().map(AdminController::toRuleVO).toList());
    }

    @PostMapping("/alerts/rules")
    public Result<Void> alertRuleCreate(@RequestBody @Valid AlertRuleUpsertDTO dto, HttpServletRequest request) {
        AlertRule rule = new AlertRule();
        rule.setRuleType(dto.getRuleType());
        rule.setThreshold(dto.getThreshold());
        rule.setEnabled(dto.getEnabled());
        rule.setRemark(dto.getRemark());
        alertRuleMapper.insert(rule);
        auditService.record(request, "ALERT_RULE", "rule-" + rule.getId(),
                "新增告警规则 " + rule.getRuleType() + " 阈值" + rule.getThreshold());
        return Result.success("新增成功", null);
    }

    @PutMapping("/alerts/rules/{id}")
    public Result<Void> alertRuleUpdate(@PathVariable Long id, @RequestBody @Valid AlertRuleUpsertDTO dto, HttpServletRequest request) {
        AlertRule row = alertRuleMapper.selectById(id);
        if (row == null) {
            throw new IllegalArgumentException("规则不存在");
        }
        row.setRuleType(dto.getRuleType());
        row.setThreshold(dto.getThreshold());
        row.setEnabled(dto.getEnabled());
        row.setRemark(dto.getRemark());
        alertRuleMapper.updateById(row);
        auditService.record(request, "ALERT_RULE", "rule-" + id, "修改告警规则 " + row.getRuleType());
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/alerts/rules/{id}")
    public Result<Void> alertRuleDelete(@PathVariable Long id, HttpServletRequest request) {
        AlertRule row = alertRuleMapper.selectById(id);
        alertRuleMapper.deleteById(id);
        auditService.record(request, "ALERT_RULE", "rule-" + id,
                row == null ? "删除告警规则 #" + id : "删除告警规则 " + row.getRuleType());
        return Result.success("删除成功", null);
    }

    // ---------- ⑫ 医保局目录下载同步（开发期对端=csmis，生产期=医保局平台） ----------

    @PostMapping("/dict/download")
    public Result<DictDownloadResultVO> dictDownload(@Valid DictDownloadQueryDTO query, HttpServletRequest request) {
        String type = query.getType();
        String msgType = switch (type) {
            case "DRUG" -> "1301";
            case "DIAG" -> "1302";
            case "MAT" -> "1303";
            default -> throw new IllegalArgumentException("不支持的目录类型：" + type + "（DRUG/DIAG/MAT）");
        };
        // 走统一转发（落 mig_gateway_log，目录下载也留痕）；响应为医保局报文原文
        String resp = forwardService.forward(msgType, "{\"catalogType\":\"" + type + "\"}");
        List<Map<String, Object>> items = parseDownloadResp(resp, type);
        // 该类型已有医保编码 → id 映射，做增量 upsert（his_code 留空，待院内目录导入后匹配）
        List<DictCache> existing = dictMapper.selectList(new LambdaQueryWrapper<DictCache>()
                .eq(DictCache::getDictType, type).isNotNull(DictCache::getMediCode));
        Map<String, Long> codeToId = new LinkedHashMap<>();
        for (DictCache d : existing) {
            if (d.getMediCode() != null) codeToId.put(d.getMediCode(), d.getId());
        }
        int imported = 0, updated = 0;
        for (Map<String, Object> it : items) {
            Object ybObj = it.get("ybCode");
            Object nameObj = it.get("name");
            if (ybObj == null || nameObj == null) continue;
            String ybCode = String.valueOf(ybObj);
            String name = String.valueOf(nameObj);
            Long id = codeToId.get(ybCode);
            if (id != null) {
                DictCache d = dictMapper.selectById(id);
                if (d != null && !name.equals(d.getMediName())) {
                    d.setMediName(name);
                    dictMapper.updateById(d);
                    updated++;
                }
            } else {
                DictCache d = new DictCache();
                d.setDictType(type);
                d.setMediCode(ybCode);
                d.setMediName(name);
                d.setMatchStatus(0);
                dictMapper.insert(d);
                imported++;
            }
        }
        auditService.record(request, "DICT_DOWNLOAD", "dict:" + type,
                "医保局目录下载 " + type + "：共" + items.size() + "条，新增" + imported + " 更新" + updated);
        DictDownloadResultVO vo = new DictDownloadResultVO();
        vo.setType(type);
        vo.setTotal(items.size());
        vo.setImported(imported);
        vo.setUpdated(updated);
        return Result.success(vo);
    }

    // ---------- 私有辅助 ----------

    /** 解析医保局目录下载响应：code=200 且 data.infcode=0，否则抛错 */
    private List<Map<String, Object>> parseDownloadResp(String resp, String type) {
        try {
            JsonNode root = objectMapper.readTree(resp);
            if (root.path("code").asInt(-1) != 200) {
                throw new IllegalArgumentException("医保局目录下载失败：" + root.path("message").asText("未知错误"));
            }
            JsonNode data = root.path("data");
            if (data.path("infcode").asInt(-1) != 0) {
                throw new IllegalArgumentException("医保局目录下载失败：" + data.path("errMsg").asText("未知错误"));
            }
            JsonNode list = data.path("list");
            if (!list.isArray()) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode n : list) {
                result.add(objectMapper.convertValue(n, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("解析医保局目录下载响应失败：" + e.getMessage(), e);
        }
    }

    private List<GatewayLog> selectToday() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        return logMapper.selectList(new LambdaQueryWrapper<GatewayLog>()
                .ge(GatewayLog::getCreatedAt, dayStart)
                .lt(GatewayLog::getCreatedAt, dayStart.plusDays(1)));
    }

    private List<GatewayLog> selectRecent() {
        return logMapper.selectList(new LambdaQueryWrapper<GatewayLog>()
                .orderByDesc(GatewayLog::getId).last("LIMIT " + RECENT_LIMIT));
    }

    /** 总笔数/成功/失败/成功率/平均耗时：今日面板与对账摘要同口径 */
    private record CountStat(long total, long success, long fail, double successRate, long avgCostMs) {
    }

    private static CountStat countOf(List<GatewayLog> rows) {
        long total = rows.size();
        long success = rows.stream().filter(l -> l.getSuccess() == 1).count();
        double avgMs = rows.stream().filter(l -> l.getCostMs() != null)
                .mapToInt(GatewayLog::getCostMs).average().orElse(0);
        return new CountStat(total, success, total - success,
                total == 0 ? 100 : Math.round(success * 1000.0 / total) / 10.0,
                Math.round(avgMs));
    }

    /** 按报文类型汇总总笔数/成功/失败，类型码升序（面板柱图与对账明细表同用） */
    private static List<MsgTypeStatVO> groupByMsgType(List<GatewayLog> rows) {
        Map<String, long[]> acc = new LinkedHashMap<>();
        for (GatewayLog r : rows) {
            long[] c = acc.computeIfAbsent(r.getMsgType(), k -> new long[3]);
            c[0]++;
            if (r.getSuccess() == 1) c[1]++;
            else c[2]++;
        }
        List<MsgTypeStatVO> list = new ArrayList<>();
        acc.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            MsgTypeStatVO vo = new MsgTypeStatVO();
            vo.setMsgType(e.getKey());
            vo.setTotal(e.getValue()[0]);
            vo.setSuccess(e.getValue()[1]);
            vo.setFail(e.getValue()[2]);
            list.add(vo);
        });
        return list;
    }

    private static <E, V> PageResult<V> pageOfVO(Page<E> page, java.util.function.Function<E, V> converter) {
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getPages(),
                page.getRecords().stream().map(converter).toList());
    }

    private static <E> Page<E> toPage(PageParam query) {
        return new Page<>(query.getPageNum(), query.getPageSize());
    }

    private static GatewayLogListVO toLogVO(GatewayLog row) {
        GatewayLogListVO vo = new GatewayLogListVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    private static AuditLogListVO toAuditVO(AuditLog row) {
        AuditLogListVO vo = new AuditLogListVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    private static DictCacheVO toDictVO(DictCache row) {
        DictCacheVO vo = new DictCacheVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    private static AlertRuleVO toRuleVO(AlertRule row) {
        AlertRuleVO vo = new AlertRuleVO();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
