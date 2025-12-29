package com.zjsyinfo.gateway.mongo.service;

import com.mongoplus.model.PageResult;
import com.mongoplus.service.IService;
import com.zjsyinfo.gateway.mongo.entity.GatewayApp;

import java.util.List;

/**
 * 应用实体的服务接口，提供查询与更新能力。
 */
public interface GatewayAppService extends IService<GatewayApp> {
    /**
     * 根据 API Key 查询应用实体。
     * @param apiKey API Key
     * @return 匹配的应用实体；不存在返回 null
     */
    GatewayApp getByApiKey(String apiKey);

    /**
     * 更新应用的最近使用时间。
     * @param appId 应用ID
     */
    void updateLastUsedTime(String appId);

    /**
     * 查询应用列表，支持按名称模糊查询，按创建时间倒序排序。
     * @param orgName 组织名称（可选，支持模糊查询）
     * @return 应用列表
     */
    List<GatewayApp> listApps(String orgName);

    /**
     * 分页查询应用列表，支持按名称模糊查询，按创建时间倒序排序。
     * @param orgName 组织名称（可选，支持模糊查询）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<GatewayApp> pageList(String orgName, int pageNum, int pageSize);
}