package com.zjsyinfo.gateway.mongo.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import lombok.Data;

import java.util.Date;

/**
 * 网关访问日志实体，映射集合 gateway_logs。
 * 记录调用耗时、状态、API Key 所属应用等信息。
 *
 * @author zt
 */
@Data
@CollectionName("gateway_logs")
public class GatewayLog {
    /**
     * 主键 _id
     */
    @ID
    private String id;

    /**
     * 日志创建时间
     */
    @CollectionField("create_time")
    private Date createTime;

    /**
     * 关联的应用ID（gateway_apps._id）
     */
    @CollectionField("app_id")
    private String appId;

    /**
     * 本次调用产生的 Token 数（如无法获取则置为 null/0，由后续完善）
     */
    @CollectionField("token_count")
    private Integer tokenCount;

    /**
     * 调用状态（success / error）
     */
    @CollectionField("status")
    private String status;

    /**
     * 请求来源 IP
     */
    @CollectionField("request_ip")
    private String requestIp;

    /**
     * 请求模型
     */
    @CollectionField("request_model")
    private String requestModel;

    /**
     * 请求路径（如：/v1/chat/completions）
     */
    @CollectionField("request_path")
    private String requestPath;

    /**
     * 响应耗时（毫秒）
     */
    @CollectionField("response_time")
    private Long responseTime;

}