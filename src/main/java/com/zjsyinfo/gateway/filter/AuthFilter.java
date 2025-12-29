package com.zjsyinfo.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjsyinfo.gateway.config.GatewayConfig;
import com.zjsyinfo.gateway.exception.AppStatusException;
import com.zjsyinfo.gateway.mongo.entity.GatewayApp;
import com.zjsyinfo.gateway.mongo.service.GatewayAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * API Key 鉴权过滤器（对齐 OpenAI Authorization: Bearer <api-key> 规范）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {
    private final ObjectMapper objectMapper;
    private final GatewayAppService gatewayAppService;

    /**
     * 鉴权过滤逻辑：
     * 1. 提取请求头 Authorization，要求前缀为 "Bearer "。
     * 2. 校验 API Key 是否存在于配置的白名单中（vllm.api-keys）。
     * 3. 失败返回 OpenAI 风格错误；成功则继续后续过滤链。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 提取 OpenAI 原生鉴权头
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return returnAuthError(response, "No API key provided");
        }

        // 校验 API Key
        String apiKey = authHeader.substring(7).trim();
        GatewayApp gatewayApp = gatewayAppService.getByApiKey(apiKey);
        if (gatewayApp == null) {
            log.warn("鉴权失败，无效 API Key");
            return returnAuthError(response, "Incorrect API key provided");
        }

        // 校验应用状态
        if (!"normal".equals(gatewayApp.getStatus())) {
            log.warn("鉴权失败，应用状态异常: appId={}, status={}", gatewayApp.getId(), gatewayApp.getStatus());
            return returnAuthError(response, "Application is disabled");
        }

        // 校验应用是否过期
        Date expireTime = gatewayApp.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            log.warn("鉴权失败，应用已过期: appId={}, expireTime={}", gatewayApp.getId(), expireTime);
            return returnAuthError(response, "Application has expired");
        }

        // 鉴权通过，透传请求
        return chain.filter(exchange);
    }

    /**
     * 返回 OpenAI 格式的鉴权错误
     */
    private Mono<Void> returnAuthError(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("message", message);
            errorMap.put("type", "invalid_request_error");
            errorMap.put("param", null);
            errorMap.put("code", "invalid_api_key");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("error", errorMap);

            byte[] errorBytes = objectMapper.writeValueAsBytes(responseMap);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBytes)));
        } catch (Exception e) {
            log.error("构造鉴权错误响应失败", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -200; // 在路由过滤器之前执行
    }
}