package com.zjsyinfo.gateway.controller.dto;

import lombok.Data;

import java.util.Date;

/**
 * 创建应用请求
 */
@Data
public class CreateAppRequest {
    /** 组织名称 */
    private String orgName;
    
    /** 过期时间 */
    private Date expireTime;
    
    /** 备注信息 */
    private String remark;
}
