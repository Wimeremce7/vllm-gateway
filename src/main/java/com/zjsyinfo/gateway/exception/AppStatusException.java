package com.zjsyinfo.gateway.exception;

/**
 * 应用状态异常（应用被禁用或已过期）
 */
public class AppStatusException extends RuntimeException {
    
    public AppStatusException(String message) {
        super(message);
    }
}