package com.zjsyinfo.gateway.controller;

import com.mongoplus.model.PageResult;
import com.zjsyinfo.gateway.controller.dto.CreateAppRequest;
import com.zjsyinfo.gateway.controller.dto.ApiResponse;
import com.zjsyinfo.gateway.mongo.entity.GatewayApp;
import com.zjsyinfo.gateway.mongo.service.GatewayAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 网关应用管理接口
 * @author zt
 */
@Slf4j
@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class GatewayAppController {
    
    private final GatewayAppService gatewayAppService;

    /**
     * 创建应用
     * @param request 创建请求
     * @return 创建结果，包含生成的 API Key
     */
    @PostMapping
    public ApiResponse<GatewayApp> createApp(@RequestBody CreateAppRequest request) {
        GatewayApp app = new GatewayApp();
        app.setOrgName(request.getOrgName());
        app.setApiKey(generateApiKey());
        app.setCreateTime(new Date());
        app.setExpireTime(request.getExpireTime());
        app.setStatus("normal");
        app.setRemark(request.getRemark());
        
        gatewayAppService.save(app);
        log.info("创建应用成功: orgName={}, apiKey={}", app.getOrgName(), app.getApiKey());
        
        return ApiResponse.success(app);
    }

    /**
     * 分页查询应用列表
     * @param orgName 组织名称（可选，支持模糊查询）
     * @param pageNum 页码（从1开始，默认1）
     * @param pageSize 每页大小（默认10）
     * @return 分页应用列表，按创建时间倒序排列
     */
    @GetMapping
    public ApiResponse<PageResult<GatewayApp>> listApps(
            @RequestParam(required = false) String orgName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        PageResult<GatewayApp> pageResult = gatewayAppService.pageList(orgName, pageNum, pageSize);
        log.info("查询应用列表成功: orgName={}, pageNum={}, pageSize={}, total={}", 
                orgName, pageNum, pageSize, pageResult.getTotalSize());
        return ApiResponse.success(pageResult);
    }

    /**
     * 查询应用详情
     * @param id 应用ID
     * @return 应用详情
     */
    @GetMapping("/{id}")
    public ApiResponse<GatewayApp> getApp(@PathVariable String id) {
        GatewayApp app = gatewayAppService.getById(id);
        if (app == null) {
            log.warn("查询失败，应用不存在: id={}", id);
            return ApiResponse.error("应用不存在");
        }
        
        log.info("查询应用成功: id={}, orgName={}", id, app.getOrgName());
        return ApiResponse.success(app);
    }

    /**
     * 修改应用信息
     * @param id 应用ID
     * @param request 修改请求
     * @return 修改结果
     */
    @PutMapping("/{id}")
    public ApiResponse<GatewayApp> updateApp(@PathVariable String id, @RequestBody CreateAppRequest request) {
        GatewayApp app = gatewayAppService.getById(id);
        if (app == null) {
            log.warn("修改失败，应用不存在: id={}", id);
            return ApiResponse.error("应用不存在");
        }
        
        app.setOrgName(request.getOrgName());
        app.setExpireTime(request.getExpireTime());
        app.setRemark(request.getRemark());
        
        gatewayAppService.updateById(app);
        log.info("修改应用成功: id={}, orgName={}", id, app.getOrgName());
        
        return ApiResponse.success(app);
    }

    /**
     * 修改应用状态
     * @param id 应用ID
     * @param status 新状态(normal/disabled)
     * @return 修改结果
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<GatewayApp> updateAppStatus(@PathVariable String id, @RequestParam String status) {
        GatewayApp app = gatewayAppService.getById(id);
        if (app == null) {
            log.warn("修改状态失败，应用不存在: id={}", id);
            return ApiResponse.error("应用不存在");
        }
        
        // 校验状态值
        if (!"normal".equals(status) && !"disabled".equals(status)) {
            return ApiResponse.error("状态值只能为 normal 或 disabled");
        }
        
        app.setStatus(status);
        gatewayAppService.updateById(app);
        log.info("修改应用状态成功: id={}, status={}", id, status);
        
        return ApiResponse.success(app);
    }

    /**
     * 删除应用
     * @param id 应用ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteApp(@PathVariable String id) {
        GatewayApp app = gatewayAppService.getById(id);
        if (app == null) {
            log.warn("删除失败，应用不存在: id={}", id);
            return ApiResponse.error("应用不存在");
        }
        
        gatewayAppService.removeById(id);
        log.info("删除应用成功: id={}, orgName={}", id, app.getOrgName());
        
        return ApiResponse.success(null);
    }

    /**
     * 生成 API Key
     * 格式: sk-live-{uuid}
     */
    private String generateApiKey() {
        return "sk-live-" + UUID.randomUUID().toString().replace("-", "");
    }
}
