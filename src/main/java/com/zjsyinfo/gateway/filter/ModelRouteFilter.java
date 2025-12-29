package com.zjsyinfo.gateway.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjsyinfo.gateway.config.GatewayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态路由过滤器：纯转发 vLLM 所有接口（含 /v1/models），无手写/改写
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRouteFilter implements GlobalFilter, Ordered {
    private final ObjectMapper objectMapper;
    private final GatewayConfig gatewayConfig;
    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        log.info("请求参数：{}", request.getQueryParams());
        // 1. 定义支持转发的接口列表
        List<String> supportPaths = List.of(
                "/v1/chat/completions",  // POST
                "/v1/completions",       // POST
                "/v1/models"             // GET
        );
        if (!supportPaths.contains(path)) {
            return chain.filter(exchange);
        }

        // 2. /v1/models 是 GET 方法，无请求体，直接路由
        if ("/v1/models".equals(path) && "GET".equals(request.getMethod().name())) {
            return routeModelsRequest(exchange, chain, request, response);
        }

        // 3. POST 接口（chat/completions/completions）：读取请求体后路由
        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> routePostRequest(exchange, chain, request, response, dataBuffer))
                .onErrorResume(e -> {
                    log.error("读取 POST 请求体失败", e);
                    return returnError(response, "Invalid request body", "invalid_request_error");
                });
    }

    /**
     * 处理 /v1/models GET 请求（纯路由，无请求体）
     */
    private Mono<Void> routeModelsRequest(ServerWebExchange exchange, GatewayFilterChain chain,
                                          ServerHttpRequest request, ServerHttpResponse response) {
        try {
            // 取默认模型的 vLLM 地址（或配置专门的 models 地址）
            String defaultModel = gatewayConfig.getModelMapping().keySet().iterator().next();
            String vllmBaseUrl = gatewayConfig.getModelMapping().get(defaultModel);

            // 构建目标 URL
            String targetUrl = vllmBaseUrl + "/v1/models";
            log.info("转发 GET /v1/models → {}", targetUrl);

            // 使用 WebClient 直接转发请求
            WebClient webClient = webClientBuilder.build();
            return webClient.method(HttpMethod.GET)
                    .uri(targetUrl)
                    .headers(headers -> {
                        // 复制原始请求头（排除 Host 等）
                        request.getHeaders().forEach((key, values) -> {
                            if (!key.equalsIgnoreCase(HttpHeaders.HOST) &&
                                !key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                                headers.addAll(key, values);
                            }
                        });
                    })
                    .exchangeToMono(clientResponse -> {
                        // 设置响应状态码
                        response.setStatusCode(clientResponse.statusCode());
                        // 复制响应头
                        clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                            if (!key.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                                response.getHeaders().addAll(key, values);
                            }
                        });
                        // 写入响应体
                        return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                    });
        } catch (Exception e) {
            log.error("路由 /v1/models 失败", e);
            return returnError(response, "Internal server error", "server_error");
        }
    }

    /**
     * 处理 POST 接口（chat/completions/completions）
     */
    private Mono<Void> routePostRequest(ServerWebExchange exchange, GatewayFilterChain chain,
                                        ServerHttpRequest request, ServerHttpResponse response,
                                        DataBuffer dataBuffer) {
        try {
            // 读取请求体字节
            byte[] requestBodyBytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(requestBodyBytes);
            DataBufferUtils.release(dataBuffer);

            // 解析 model 字段
            Map<String, Object> requestBody = objectMapper.readValue(
                    requestBodyBytes, new TypeReference<Map<String, Object>>() {});
            String model = (String) requestBody.get("model");
            if (model == null || model.trim().isEmpty()) {
                return returnError(response, "Missing required parameter: model", "invalid_request_error");
            }

            // 获取 model 对应的 vLLM 地址
            String vllmBaseUrl = gatewayConfig.getModelMapping().get(model.trim());
            if (vllmBaseUrl == null) {
                return returnError(response, "Model '" + model + "' not found", "model_not_found");
            }

            // 构建目标 URL
            String targetUrl = vllmBaseUrl + request.getPath().value();
            log.info("转发 POST {} → model={}, targetUrl={}", request.getPath(), model, targetUrl);

            // 使用 WebClient 直接转发请求
            WebClient webClient = webClientBuilder.build();
            return webClient.method(request.getMethod())
                    .uri(targetUrl)
                    .headers(headers -> {
                        // 复制原始请求头（排除 Host 等）
                        request.getHeaders().forEach((key, values) -> {
                            if (!key.equalsIgnoreCase(HttpHeaders.HOST)) {
                                headers.addAll(key, values);
                            }
                        });
                        headers.setContentLength(requestBodyBytes.length);
                    })
                    .bodyValue(requestBodyBytes)
                    .exchangeToMono(clientResponse -> {
                        // 设置响应状态码
                        response.setStatusCode(clientResponse.statusCode());
                        // 复制响应头
                        clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                            if (!key.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                                response.getHeaders().addAll(key, values);
                            }
                        });
                        // 写入响应体（支持流式响应）
                        return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                    });
        } catch (Exception e) {
            log.error("路由 POST 请求失败", e);
            return returnError(response, "Internal server error", "server_error");
        }
    }

    /**
     * 返回 OpenAI 格式错误（仅用于路由失败场景）
     */
    private Mono<Void> returnError(ServerHttpResponse response, String message, String type) {
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("message", message);
            errorDetail.put("type", type);
            errorDetail.put("param", null);
            errorDetail.put("code", null);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", errorDetail);

            byte[] errorBytes = objectMapper.writeValueAsBytes(errorResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBytes)));
        } catch (Exception e) {
            log.error("构造错误响应失败", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100; // 在鉴权过滤器之后执行
    }
}