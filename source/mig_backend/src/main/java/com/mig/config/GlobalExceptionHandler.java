package com.mig.config;

import com.mig.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：业务异常（IllegalArgumentException）返回 code!=200 的统一体，
 * 前端 request.js 按 cslk 协议读取 message 展示。
 * <p>
 * 补充参数校验/绑定/类型类 handler：Controller 改成 DTO 入参 + Bean Validation 后，
 * 这些异常若被 Exception 兜底吃掉，会把「调用方参数错了」渲染成 HTTP 500「系统繁忙」，
 * 掩盖真实原因，故逐个单独接住返回 4xx 语义。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /**
     * @RequestBody DTO 校验失败（@Valid 不通过）。
     * <p>
     * 必须单独接住，否则被 Exception 兜底成 500；与 cslk 一致：HTTP 200 + 业务码 400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        return Result.error(400, message);
    }

    /**
     * 表单/查询参数绑定到 DTO 时校验失败。
     * <p>
     * 同上，避免被兜底成 500，前端拿不到具体字段错误。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数绑定失败";
        return Result.error(400, message);
    }

    /**
     * 参数类型不匹配（如把 id=abc 传给 Long）。
     * <p>
     * 是调用方的问题，不是服务端故障，必须单独接住否则被兜底成 500。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}={}", e.getName(), e.getValue());
        return Result.error(400, "参数类型不正确：" + e.getName());
    }

    /**
     * 缺少必填请求参数。
     * <p>
     * 必须单独接住，否则「调用方少传了参数」看起来像「服务端炸了」。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.error(400, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 请求体无法解析（如 JSON 格式错误、字段类型对不上）。
     * <p>
     * 同样是调用方的问题，应返回 400 而不是 500。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(400, "请求体格式不正确，无法解析");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[全局异常] {}", e.getMessage(), e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
