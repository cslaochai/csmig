package com.mig.config;

import com.mig.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常（IllegalArgumentException）返回 code!=200 的统一体，
 * 前端 request.js 按 cslk 协议读取 message 展示。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[全局异常] {}", e.getMessage(), e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
