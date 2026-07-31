# Agent AI Server — 智能设备推荐与知识答疑 AI 服务

基于 **Spring Boot 3.2 + Spring AI 1.1** 构建的企业级 AI 对话服务，集成 **RAG 向量检索** + **Tool Calling**，提供智能设备推荐和领域知识问答能力。

---

## Web UI 预览

配套前端仓库：[spring_ai_Iot_agent_web](https://github.com/2101952621/spring_ai_Iot_agent_web)

### 登录页

![登录页](../docs/images/login.png)

### 对话首页

![对话首页](../../blob/main/docs/images/chat-home.png)

![对话首页-示例](../../spring_ai_Iot_agent/docs/images/chat-home-2.png)

### 知识问答

![知识问答](../../spring_ai_Iot_agent/docs/images/chat-knowledge.png)

### 设备推荐

![设备推荐](../../spring_ai_Iot_agent/docs/images/chat-recommend.png)

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | **17** | 运行环境 |
| Spring Boot | **3.2.4** | 核心框架 |
| Spring AI | **1.1.8** | AI 编排（OpenAI 兼容接口） |
| Spring Security | 6.x | JWT 无状态认证 |
| Spring Data JPA | 3.x | ORM 持久化 |
| Spring Data Redis | 3.x | 会话状态 & 缓存 |
| Elasticsearch | **8.18.8** | 向量存储 + RAG 检索 |
| PostgreSQL | — | 业务数据存储 |
| Redis | — | 缓存 & 生成状态控制 |
| LLM | **Qwen3.7-Plus / Qwen3.7-Text-Embedding** | 阿里云 DashScope |
| JWT | jjwt **0.12.5** | Token 认证 |
| OpenAPI | springdoc **2.3.0** | Swagger 文档 |
| 工具库 | Hutool **5.8.28**、Lombok | — |
| 邮件 | Spring Boot Mail（QQ SMTP） | 注册激活 & 密码重置 |
| 构建 | Maven | — |

---

## 系统架构

```
                  前端 (Vue/React)
                       │
                       │ SSE / REST API
                       ▼
              ┌─────────────────┐
              │  Controller 层   │
              │  6个 RestController│
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │   Service 层     │
              │  Agent/User/Auth │
              └────────┬────────┘
                       │
              ┌────────▼────────┐
              │ AgentOrchestrator│ ← 意图路由（策略模式）
              └────────┬────────┘
          ┌────────────┼────────────┐
          ▼            ▼            ▼
   ┌───────────┐ ┌───────────┐ ┌───────────┐
   │ RouteAgent│ │RecommendAgent│ │KnowledgeAgent│
   │ (意图分类) │ │(设备推荐+RAG) │ │ (知识答疑) │
   └───────────┘ └─────┬─────┘ └───────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
   ┌──────────┐ ┌─────────┐ ┌───────────┐
   │ ES向量存储│ │Tool Call│ │ PostgreSQL │
   │  (RAG)   │ │(设备查询)│ │  (业务库)  │
   └──────────┘ └─────────┘ └───────────┘
```

### 核心设计模式

| 模式 | 位置 | 说明 |
|------|------|------|
| **模板方法** | `AbstractBaseAgent` | 定义流式处理骨架，子类覆盖钩子方法 |
| **策略模式** | `AgentOrchestrator` | 根据意图分发给不同 Agent |
| **注册表模式** | `AgentOrchestrator` | 自动收集所有 `BaseAgent` 实现 |
| **Advisor 链** | `SpringAIConfig` | 日志 → 记录优化 → 对话记忆 |

---

## 项目结构

```
src/main/java/com/ai/server/
├── agent/                        # AI 智能体核心
│   ├── BaseAgent.java            # Agent 接口
│   ├── AbstractBaseAgent.java    # 抽象基类（模板方法）
│   ├── RouteBaseAgent.java       # 意图路由智能体
│   ├── RecommendBaseAgent.java   # 设备推荐智能体（RAG）
│   ├── KnowledgeBaseAgent.java   # 知识答疑智能体
│   ├── advisor/                  # Advisor 拦截器
│   ├── enums/                    # 枚举（AgentType、EventType）
│   ├── memory/                   # 持久化记忆（PostgreSQL）
│   ├── orchestrator/             # 中央编排器
│   └── tools/                    # Tool Calling 工具
├── common/
│   └── exception/                # 全局异常处理
├── config/                       # 配置类（AI/ES/Redis/Swagger）
├── controller/                   # REST 控制器（6个）
├── model/
│   ├── entity/                   # JPA 实体（5张表）
│   ├── dto/                      # 请求 DTO
│   └── vo/                       # 响应 VO
├── repository/                   # JPA Repository（5个）
├── security/                     # 安全（JWT Filter + Provider）
└── service/                      # 业务服务层
    ├── ai/                       # AI 相关服务
    └── user/                     # 用户相关服务
```

---

## 环境准备

### 必需依赖

- **JDK 17+**
- **Maven 3.6+**
- **PostgreSQL**（业务数据库）
- **Redis**（会话状态 & 缓存）
- **Elasticsearch 8.x**（向量存储 & RAG 检索）

### API Key

- **阿里云 DashScope API Key**（用于 Qwen 模型调用）
    - 获取地址：[https://dashscope.aliyun.com/](https://dashscope.aliyun.com/)

---

## 部署步骤

### 1. 执行数据库脚本配置数据库
### 2. 配置 Elasticsearch
> 运行前需确保 ES 已安装并启动（推荐 ES 9.x），创建索引后配置向量存储。
- **ES 启动**后，确保可访问 `http://localhost:9200`
- 向量索引将由 Spring AI 启动时自动创建（`initialize-schema: true`）
- 需要在 `application.yml` 中配置 ES 连接信息：
### 3. 配置 AI Key 与模型
> 在 `application.yml` 中配置阿里云 DashScope API Key 和模型名称。
### 4. 配置 Redis
### 5. 配置其他服务(非必要)
邮件服务

启动成功后访问：
- **Swagger 文档**：[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **API 接口基路径**：`http://localhost:8080/api/`

---

## 前端项目

前端（Vue + Element Plus）独立仓库，负责 UI 交互和 SSE 流式渲染：

> **GitHub**: [https://github.com/2101952621/spring_ai_Iot_agent_web](https://github.com/2101952621/spring_ai_Iot_agent_web)

部署时需将前端的 API 请求地址指向本服务的 `http://localhost:8080`。

---

## API 接口概览

| 模块 | 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|------|
| AI 对话 | POST | `/api/ai/chat` | SSE 流式对话 | ✅ |
| AI 对话 | POST | `/api/ai/stop/{sessionId}` | 停止生成 | ✅ |
| 会话管理 | GET | `/api/ai/history` | 查询历史会话（按周/月分组） | ✅ |
| 会话管理 | DELETE | `/api/ai/history` | 删除指定会话 | ✅ |
| 会话管理 | PUT | `/api/ai/history` | 更新会话标题 | ✅ |
| 热门示例 | GET | `/api/ai/hot` | 获取热门示例 | ✅ |
| 认证 | POST | `/api/auth/login` | 用户登录 | ❌ |
| 认证 | POST | `/api/auth/token` | 刷新 Token | ❌ |
| 认证 | PUT | `/api/auth/password` | 修改密码 | ✅ |
| 认证 | DELETE | `/api/auth/user` | 注销账号 | ✅ |
| 用户 | GET | `/api/auth/me` | 获取当前用户信息 | ✅ |
| 注册 | POST | `/api/customer/register` | 发送注册邮件 | ❌ |
| 注册 | POST | `/api/customer/register-by-mail` | 邮箱注册 | ❌ |
| 激活 | POST | `/api/noauth/activate` | 激活账号 | ❌ |
| 密码重置 | POST | `/api/noauth/reset-password` | 发送密码重置邮件 | ❌ |
| 密码重置 | POST | `/api/noauth/reset-password/check` | 验证重置令牌 | ❌ |
| 密码重置 | POST | `/api/noauth/reset-password/reset` | 重置密码 | ❌ |

> Token 通过请求头 `X-Authorization: Bearer <token>` 传递

## 安全提示

1. **生产环境** 务必通过环境变量覆盖 `JWT_SECRET` 和 `api-key`
2. 邮件密码建议使用邮箱 **授权码**，而非登录密码
3. Redis 生产环境需设置密码并限制访问来源
4. ES 生产环境需开启 HTTPS 并配置安全证书
5. 建议使用 Nginx 反向代理对外暴露服务
