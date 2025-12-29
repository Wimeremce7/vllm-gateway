package com.zjsyinfo.gateway.mongo.service.impl;

import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import com.zjsyinfo.gateway.mongo.entity.GatewayLog;
import com.zjsyinfo.gateway.mongo.service.GatewayLogService;
import org.springframework.stereotype.Service;

/**
 * 访问日志服务实现。
 */
@Service
public class GatewayLogServiceImpl extends ServiceImpl<GatewayLog> implements GatewayLogService {
    
    /**
     * 分页查询日志列表，支持按应用ID过滤，按创建时间倒序排序。
     */
    @Override
    public PageResult<GatewayLog> pageList(String appId, int pageNum, int pageSize) {
        return this.lambdaQuery()
                .eq(appId != null && !appId.isEmpty(), GatewayLog::getAppId, appId)
                .orderByDesc(GatewayLog::getCreateTime)
                .page(pageNum, pageSize);
    }
}