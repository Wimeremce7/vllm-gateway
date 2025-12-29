# vllm-gateway

## 项目简介

vllm-gateway 是一个基于 Spring Cloud Gateway 构建的 AI 模型网关，专门用于对接 vLLM 服务。该网关实现了 OpenAI API 兼容的接口，支持多模型路由、API Key 鉴权、访问日志记录等功能，为 AI 模型服务提供统一的入口管理和监控。

## 功能特性

- **API Key 鉴权**：支持 OpenAI 规范的 `Authorization: Bearer <api-key>` 鉴权方式
- **多模型路由**：根据请求中的 `model` 参数动态路由到不同的 vLLM 实例
- **访问控制**：支持应用状态管理（启用/禁用）、过期时间控制
- **访问日志**：记录每次调用的详细信息，包括 token 数量、响应时间、状态等
- **OpenAI 兼容**：完全兼容 OpenAI API 接口（chat/completions、completions、models）
- **流式响应**：支持 SSE 流式输出，实时提取 token 统计信息

## 技术栈

- **Spring Boot 3.2.5**
- **Spring Cloud Gateway**
- **MongoDB**（使用 Mongo-Plus 框架）
- **Lombok**（简化代码）
- **Reactor Netty**（异步非阻塞）

## 环境要求

- **Java**: 21+
- **MongoDB**: 4.0+
- **vLLM**: 支持 OpenAI API 格式的模型服务

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/你的用户名/vllm-gateway.git
cd vllm-gateway
```

### 2. 配置环境

修改 `src/main/resources/application-dev.yml` 文件，配置 MongoDB 连接信息和 vLLM 模型映射：

```yaml
mongo-plus:
  data:
    mongodb:
      host: 127.0.0.1
      port: 27017
      database: xxx
      username: xxx
      password: xxx

vllm:
  model-mapping:
    Qwen3-235B: xxx
    Qwen3-32B: xxx
    Qwen2.5-VL-72B: xxx
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

### 4. 创建应用并获取 API Key

通过以下接口创建应用并获取 API Key：

```bash
curl -X POST http://localhost:8080/api/apps \
  -H "Content-Type: application/json" \
  -d '{
    "orgName": "my-org",
    "expireTime": "2024-12-31T23:59:59.000+08:00",
    "remark": "测试应用"
  }'
```

## API 接口

### 网关应用管理

- `POST /api/apps` - 创建应用
- `GET /api/apps` - 分页查询应用列表
- `GET /api/apps/{id}` - 查询应用详情
- `PUT /api/apps/{id}` - 修改应用信息
- `PATCH /api/apps/{id}/status` - 修改应用状态
- `DELETE /api/apps/{id}` - 删除应用

### 访问日志查询

- `GET /api/logs` - 分页查询访问日志

### 目前支持的vLLM API 接口

- `POST /v1/chat/completions` - 聊天补全
- `POST /v1/completions` - 文本补全
- `GET /v1/models` - 获取模型列表

## 使用示例

使用 API Key 调用 vLLM 服务：

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer sk-live-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "Qwen3-32B",
    "messages": [
      {
        "role": "user",
        "content": "你好"
      }
    ]
  }'
```

## 核心组件

### 鉴权过滤器

- **`AuthFilter`**：实现 API Key 鉴权，验证应用状态和过期时间
- **`AppStatusException`**：应用状态异常处理

### 路由过滤器

- **`ModelRouteFilter`**：根据模型名称动态路由到对应的 vLLM 实例
- **`AccessLogFilter`**：记录访问日志并统计 token 使用情况

### 数据模型

- **`GatewayApp`**：网关应用实体，存储 API Key 和应用信息
- **`GatewayLog`**：访问日志实体，记录调用详情

### 服务层

- **`GatewayAppService`**：应用管理服务
- **`GatewayLogService`**：日志查询服务

## 项目结构

```
src/
├── main/
│   ├── java/com/zjsyinfo/gateway/
│   │   ├── controller/          # API 控制器
│   │   ├── filter/              # 网关过滤器
│   │   ├── exception/           # 异常处理
│   │   ├── config/              # 配置类
│   │   ├── mongo/               # MongoDB 相关
│   │   └── VllmGatewayApplication.java
│   └── resources/
│       ├── application.yml      # 主配置文件
│       └── application-dev.yml  # 开发环境配置
```

## 过滤器链

1. **`AuthFilter`**：API Key 鉴权
2. **`ModelRouteFilter`**：模型路由
3. **`AccessLogFilter`**：访问日志记录

## 贡献

欢迎提交 Issue 和 Pull Request！
