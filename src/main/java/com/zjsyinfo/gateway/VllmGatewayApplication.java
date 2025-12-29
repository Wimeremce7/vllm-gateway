package com.zjsyinfo.gateway;

import com.mongoplus.annotation.MongoMapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties // 启用配置绑定
@MongoMapperScan("com.zjsyinfo.gateway.mongo.mapper")
public class VllmGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(VllmGatewayApplication.class, args);
    }

}
