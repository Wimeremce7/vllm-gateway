package com.zjsyinfo.gateway.mongo.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import lombok.Data;

import java.util.Date;

/**
 * 网关应用实体，映射集合 gateway_apps。
 * 用于记录接入方信息，以及通过 api_key 进行关联。
 */
@Data
@CollectionName("gateway_apps")
public class GatewayApp {
    /**
     * MongoDB 主键，对应文档 _id。
     * 默认使用 ObjectId 类型，由框架自动生成。
     */
    @ID
    private String id;

    /** 组织名称 */
    @CollectionField("org_name")
    private String orgName;

    /** 应用关联的 API Key（用于鉴权与日志关联） */
    @CollectionField("api_key")
    private String apiKey;

    /** 创建时间 */
    @CollectionField("create_time")
    private Date createTime;

    //TODO 增加模型字段（开通的权限模型）

    /** 过期时间 */
    @CollectionField("expire_time")
    private Date expireTime;

    /** 状态（如：normal、disabled 等） */
    @CollectionField("status")
    private String status;

    /** 备注信息 */
    @CollectionField("remark")
    private String remark;

    /** 最近一次使用时间（调用发生时更新） */
    @CollectionField("last_used_time")
    private Date lastUsedTime;
}