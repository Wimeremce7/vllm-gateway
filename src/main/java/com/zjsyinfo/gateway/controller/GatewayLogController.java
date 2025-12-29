package com.zjsyinfo.gateway.controller;

import com.mongoplus.model.PageResult;
import com.zjsyinfo.gateway.controller.dto.ApiResponse;
import com.zjsyinfo.gateway.mongo.entity.GatewayLog;
import com.zjsyinfo.gateway.mongo.service.GatewayLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 网关日志查询接口
 * @author zt
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class GatewayLogController {
    
    private final GatewayLogService gatewayLogService;

    /**
     * 分页查询日志列表
     * @param appId 应用ID（可选，支持按应用ID过滤）
     * @param pageNum 页码（从1开始，默认1）
     * @param pageSize 每页大小（默认10）
     * @return 分页日志列表，按创建时间倒序排列
     */
    @GetMapping
    public ApiResponse<PageResult<GatewayLog>> listLogs(
            @RequestParam(required = false) String appId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        PageResult<GatewayLog> pageResult = gatewayLogService.pageList(appId, pageNum, pageSize);
        log.info("查询日志列表成功: appId={}, pageNum={}, pageSize={}, total={}", 
                appId, pageNum, pageSize, pageResult.getTotalSize());
        
        return ApiResponse.success(pageResult);
    }
}
