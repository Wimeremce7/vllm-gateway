package com.zjsyinfo.gateway.exception;

import com.zjsyinfo.gateway.controller.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", errorMessage);
        return ApiResponse.error("参数错误: " + errorMessage);
    }

    /**
     * 处理应用状态异常（应用被禁用或已过期）
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AppStatusException.class)
    public ApiResponse<Void> handleAppStatusException(AppStatusException e) {
        log.warn("应用状态异常: {}", e.getMessage());
        return ApiResponse.error(e.getMessage());
    }

    /**
     * 处理通用异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneralException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统内部错误，请联系管理员");
    }
}