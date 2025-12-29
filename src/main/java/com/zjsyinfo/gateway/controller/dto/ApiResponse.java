package com.zjsyinfo.gateway.controller.dto;

import lombok.Data;

/**
 * 统一 API 响应
 */
@Data
public class ApiResponse<T> {
    /** 状态码：200 成功，其他失败 */
    private Integer code;
    
    /** 响应消息 */
    private String message;
    
    /** 响应数据 */
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
}
