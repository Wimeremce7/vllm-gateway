package com.zjsyinfo.gateway.mongo.service;

import com.mongoplus.model.PageResult;
import com.mongoplus.service.IService;
import com.zjsyinfo.gateway.mongo.entity.GatewayLog;

/**
 * 访问日志服务接口。
 */
public interface GatewayLogService extends IService<GatewayLog> {
    /**
     * 分页查询日志列表，支持按应用ID过滤，按创建时间倒序排序。
     * @param appId 应用ID（可选）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<GatewayLog> pageList(String appId, int pageNum, int pageSize);
}