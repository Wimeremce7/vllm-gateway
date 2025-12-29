package com.zjsyinfo.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 加载自定义配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vllm")
public class GatewayConfig {
    /** model -> vLLM 地址映射 */
    private Map<String, String> modelMapping;

    /**
     * 配置 WebClient，用于转发请求到 vLLM
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // 配置 HttpClient 以支持长时间连接和流式响应
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .responseTimeout(Duration.ofMinutes(30))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.MINUTES))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.MINUTES)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}