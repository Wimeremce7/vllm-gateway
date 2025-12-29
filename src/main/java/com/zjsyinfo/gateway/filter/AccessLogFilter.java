package com.zjsyinfo.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjsyinfo.gateway.mongo.entity.GatewayApp;
import com.zjsyinfo.gateway.mongo.entity.GatewayLog;
import com.zjsyinfo.gateway.mongo.service.GatewayAppService;
import com.zjsyinfo.gateway.mongo.service.GatewayLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * 访问日志过滤器：记录每次调用的关键指标到 MongoDB（gateway_logs）。
 * 采集字段：create_time, app_id, token_count(可空), status, request_ip, request_path, response_time。
 * @author zt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogFilter implements GlobalFilter, Ordered {
    private final GatewayAppService gatewayAppService;
    private final GatewayLogService gatewayLogService;
    private final ObjectMapper objectMapper;

    /**
     * 过滤链：记录耗时与调用状态，并尝试从响应体中解析 usage.total_tokens 作为 token_count。
     * 注意：不修改响应语义，仅复制原始响应内容后写回。
     * @param exchange 当前请求/响应上下文
     * @param chain    过滤器链
     * @return 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();

        // 提取 IP
        InetSocketAddress remoteAddr = request.getRemoteAddress();
        String requestIp = remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : "unknown";

        // 提取 API Key（OpenAI 规范：Authorization: Bearer <key>）
        String authHeader = request.getHeaders().getFirst("Authorization");
        final String apiKey;  // 声明为 final 以便在 lambda 中使用
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7).trim();
        } else {
            apiKey = null;
        }
        // 装饰响应以便解析响应体，从而记录 token_count 与状态
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            private final StringBuilder fullResponse = new StringBuilder();
            private Integer totalTokens = null;

            /**
             * 拦截响应写出过程，提取响应体并复制写回，同时记录访问日志。
             */
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.doOnNext(dataBuffer -> {
                        // 使用 slice 读取而不消耗 buffer，保持流式传输
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.slice(0, dataBuffer.readableByteCount()).read(content);
                        String chunk = new String(content, StandardCharsets.UTF_8);
                        fullResponse.append(chunk);

                        // 尝试从当前 chunk 提取 token（流式输出时每个 chunk 都可能包含 usage）
                        Integer tokens = extractTokensFromChunk(chunk);
                        if (tokens != null) {
                            totalTokens = tokens;
                        }
                    }).doFinally(signalType -> {
                        // 流结束时记录日志
                        String status = (getStatusCode() != null && getStatusCode().is2xxSuccessful()) ? "success" : "error";

                        // 如果之前没有从 chunk 中提取到，尝试从完整响应体提取（非流式情况）
                        if (totalTokens == null) {
                            totalTokens = extractTokensFromResponse(fullResponse.toString());
                        }

                        writeAccessLog(apiKey, requestIp, path, System.currentTimeMillis() - start, status, totalTokens, null);
                    }));
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build()).doOnError(err -> {
            // 异常场景也记录日志
            writeAccessLog(apiKey, requestIp, path, System.currentTimeMillis() - start, "error", null,null);
        });
    }

    /**
     * 从 SSE chunk 中提取 token 信息（流式输出）
     * OpenAI 流式格式：data: {"choices":[...],"usage":{"total_tokens":123}}
     */
    private Integer extractTokensFromChunk(String chunk) {
        try {
            // SSE 格式处理：提取 data: 后面的 JSON
            String[] lines = chunk.split("\n");
            for (String line : lines) {
                if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                    String json = line.substring(6).trim();
                    if (!json.isEmpty()) {
                        Map<?, ?> data = objectMapper.readValue(json, Map.class);
                        Object usage = data.get("usage");
                        if (usage instanceof Map<?, ?> usageMap) {
                            Object totalTokens = usageMap.get("total_tokens");
                            if (totalTokens instanceof Number) {
                                return ((Number) totalTokens).intValue();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 从完整响应体中提取 token 信息（非流式输出）
     * OpenAI 非流式格式：{"choices":[...],"usage":{"total_tokens":123}}
     */
    private Integer extractTokensFromResponse(String respBody) {
        try {
            Map<?, ?> root = objectMapper.readValue(respBody, Map.class);
            Object usage = root.get("usage");
            if (usage instanceof Map<?, ?> usageMap) {
                Object totalTokens = usageMap.get("total_tokens");
                if (totalTokens instanceof Number) {
                    return ((Number) totalTokens).intValue();
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 写入访问日志，并更新应用的最近使用时间。
     * @param apiKey      请求使用的 API Key
     * @param requestIp   请求来源 IP
     * @param path        请求路径
     * @param durationMs  响应耗时（毫秒）
     * @param status      调用状态（success/error）
     * @param tokenCount  本次调用产生的 Token 数（如无法获取则为 null）
     */
    private void writeAccessLog(String apiKey, String requestIp, String path, long durationMs,
                                String status, Integer tokenCount, String model) {
        try {
            String appId = null;
            if (apiKey != null && !apiKey.isEmpty()) {
                GatewayApp app = gatewayAppService.getByApiKey(apiKey);
                if (app != null) {
                    appId = app.getId();
                    // 更新最近使用时间
                    gatewayAppService.updateLastUsedTime(appId);
                }
            }

            GatewayLog logDoc = new GatewayLog();
            logDoc.setCreateTime(new Date());
            logDoc.setAppId(appId);
            logDoc.setTokenCount(tokenCount);
            logDoc.setStatus(status);
            logDoc.setRequestIp(requestIp);
            logDoc.setRequestPath(path);
            logDoc.setResponseTime(durationMs);
            logDoc.setRequestModel(model);

            gatewayLogService.save(logDoc);
        } catch (Exception e) {
            // 不影响主流程，仅记录错误
            log.error("写入访问日志失败", e);
        }
    }

    @Override
    public int getOrder() {
        // 在路由与鉴权过滤器之后执行，确保能拿到状态码与响应体
        return -101;
    }
}