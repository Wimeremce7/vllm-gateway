package com.zjsyinfo.gateway.mongo.service.impl;

import com.mongoplus.model.PageResult;
import com.zjsyinfo.gateway.mongo.entity.GatewayApp;
import com.zjsyinfo.gateway.mongo.service.GatewayAppService;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 应用实体服务实现。
 * 说明：基于 Mongo-Plus 的通用 Service 能力实现查询与更新。
 */
@Service
public class GatewayAppServiceImpl extends ServiceImpl<GatewayApp> implements GatewayAppService {

    /**
     * 根据 API Key 查询应用实体。
     */
    @Override
    public GatewayApp getByApiKey(String apiKey) {
        return this.lambdaQuery()
                .eq(GatewayApp::getApiKey, apiKey)
                .one();
    }

    /**
     * 更新应用的最近使用时间。
     */
    @Override
    public void updateLastUsedTime(String appId) {
        if (appId == null) {
            return;
        }
        GatewayApp app = this.getById(appId);
        if (app != null) {
            app.setLastUsedTime(new java.util.Date());
            // 仅更新实体，不创建新纪录
            this.updateById(app);
        }
    }

    /**
     * 查询应用列表，支持按名称模糊查询，按创建时间倒序排序。
     */
    @Override
    public List<GatewayApp> listApps(String orgName) {
        return this.lambdaQuery()
                .like(orgName != null && !orgName.isEmpty(), GatewayApp::getOrgName, orgName)
                .orderByDesc(GatewayApp::getCreateTime)
                .list();
    }

    /**
     * 分页查询应用列表，支持按名称模糊查询，按创建时间倒序排序。
     */
    @Override
    public PageResult<GatewayApp> pageList(String orgName, int pageNum, int pageSize) {
        return this.lambdaQuery()
                .like(orgName != null && !orgName.isEmpty(), GatewayApp::getOrgName, orgName)
                .orderByDesc(GatewayApp::getCreateTime)
                .page(pageNum, pageSize);
    }
}