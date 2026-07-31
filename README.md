# Agent AI Server — 打造一站式企业物联网云智能AI解决方案。目前支持:智能设备推荐与知识答疑AI服务

基于 **Spring Boot 3.2 + Spring AI 1.1** 构建的企业级 AI 服务，集成 **RAG 向量检索** + **Tool Calling**，提供智能设备推荐和领域知识问答能力。

---

## Web UI 预览

配套前端仓库：[spring_ai_Iot_agent_web](https://github.com/2101952621/spring_ai_Iot_agent_web)
<img width="1868" height="915" alt="user_login" src="https://github.com/user-attachments/assets/7208c488-1925-48a7-b071-802fb84d9f84" />

<img width="1866" height="909" alt="图片" src="https://github.com/user-attachments/assets/03ddbbf2-ec0d-4370-a544-9e0bfc6822f1" />

<img width="1872" height="897" alt="login_in" src="https://github.com/user-attachments/assets/e8a9d89e-7f3d-4ad8-a157-d53de2be58f3" />

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
              │  RestController │
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

## Agent 推荐设备 — 架构流程详解
### 一、整体请求处理时序
```
用户发送 "推荐一款SOHO办公路由器"
        │
        ▼
  AgentController (SSE)
        │
        ▼
  AgentOrchestrator.orchestrate()
        │
        ├─ ① 会话更新：agentService.update() → 创建/更新会话记录
        │
        ├─ ② 意图路由：RouteAgent.process() 同步调用
        │     根据 ROUTER 系统 Prompt 返回 "RECOMMEND"
        │
        ├─ ③ 策略分发：AgentTypeEnum.agentNameOf("RECOMMEND")
        │     从注册表 agentRegistry 获取 RecommendBaseAgent
        │
        └─ ④ 流式执行：targetAgent.processStream()
               ┌─ RAG 向量检索
               ├─ LLM 推理 + Tool Calling
               └─ SSE 流式输出
```
---
### 二、意图路由阶段（RouteBaseAgent）
1. 用户问题被送入 `RouteBaseAgent`，使用 `SystemConstant.ROUTER` 作为系统 Prompt。
2. LLM 分析用户意图，**仅输出**以下关键词之一：
  - `RECOMMEND` — 设备推荐意图（包含使用场景 + 关键需求关键词）
  - `KNOWLEDGE` — 知识答疑意图
  - 问候语友好回应
  - 兜底拒答文本
3. `AgentOrchestrator` 根据返回文本匹配 `AgentTypeEnum`，从 Agent 注册表中获取对应 Agent 实例。
4. 若意图匹配失败或 Agent 未注册，返回兜底响应流。
```
RouteAgent.process() 同步输出示例：
  "推荐一款SOHO办公路由器"    →  "RECOMMEND"
  "交换机有什么作用"           →  "KNOWLEDGE"
  "你好"                     →  "您好！有什么可以帮您？"
  "今天天气"                  →  "抱歉我只处理平台相关问题"
```
---
### 三、推荐 Agent 执行流程（RecommendBaseAgent）
`RecommendBaseAgent` 是设备推荐的核心智能体，继承自 `AbstractBaseAgent`（模板方法模式），集成了 **RAG 向量检索** 和 **Tool Calling** 两大能力。
#### 3.1 流程概览
```
RecommendBaseAgent.processStream()
  │
  ├─ generateRequestId()              → 生成唯一 requestId
  ├─ beforeProcessStream()            → 记录请求日志（钩子）
  │
  ├─ ChatClient 请求构建
  │    ├─ .system(SystemConstant.RECOMMEND)    → 设备推荐 System Prompt
  │    ├─ .advisors(QuestionAnswerAdvisor)     → RAG 向量检索 Advisor
  │    ├─ .tools(DeviceBaseInfoTools)          → Tool Calling 设备查询
  │    ├─ .toolContext(sessionId, requestId)   → 工具上下文透传
  │    └─ .user(question)                      → 用户问题
  │
  ├─ .stream().chatResponse()          → 流式执行
  │    │
  │    ├─ doFirst: Redis 设置生成状态  → chat:generate_status:{sessionId} = true
  │    │
  │    ├─ takeWhile: 每帧检查生成状态  → 用户可随时中断（stop API）
  │    │
  │    ├─ concatMap: transformChatResponse()
  │    │    ├─ 解析 ChatResponse、提取文本
  │    │    ├─ finishReason == STOP 时缓存 Tool 调用结果
  │    │    └─ 输出 DATA / PARAM / STOP 事件
  │    │
  │    ├─ doOnCancel: 保存中断记录      → chatMemory.add(partialContent)
  │    │
  │    └─ doFinally: 清理 Redis + afterProcessStream()
  │
  └─ SSE 事件流输出
```

#### 3.2 流式输出的 SSE 事件类型

| Event Type | Code | 含义 | 携带内容 |
|------------|------|------|----------|
| `DATA` | 1001 | LLM 生成文本片段 | `eventData`: 文本块 |
| `PARAM` | 1003 | Tool 调用结果参数 | `eventData`: Map（设备详情JSON） |
| `STOP` | 1002 | 流结束标记 | 无 |

**输出顺序：** `DATA... → (PARAM) → STOP`

当 Tool Calling 返回了设备详情时，在 `STOP` 之前会额外发送一个 `PARAM` 事件，将设备的结构化数据传递给前端。

---

### 四、RAG 向量检索详解

#### 4.1 配置参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 向量存储 | **Elasticsearch 8.x** | 索引名 `device_base_info_vectors` |
| Embedding 模型 | `qwen3.7-text-embedding` | 阿里云 DashScope，1024 维 |
| 相似度算法 | `cosine` | 余弦相似度 |
| TopK | 6 | 每次检索返回 Top 6 候选 |
| 相似度阈值 | 0.6 | 低于此分数的文档不参与推荐 |

#### 4.2 检索机制

`RecommendBaseAgent` 通过 `QuestionAnswerAdvisor`（Spring AI 内置 RAG Advisor）在 Advisor 链中注入向量检索：

```java
// RecommendBaseAgent.advisors()
var qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
        .searchRequest(SearchRequest.builder()
                .similarityThreshold(0.6d)
                .topK(6)                     
                .build())
        .build();
```

**执行时机：** 在 LLM 收到用户问题之前，`QuestionAnswerAdvisor` 先对用户问题进行向量化 → ES 相似度检索 → 将命中的设备资料作为上下文注入到 Prompt 中 → 再交由 LLM 推理。

#### 4.3 启动健康检查

应用启动时，`RecommendBaseAgent.debugVectorStore()` 通过 `@PostConstruct` 自动对测试关键词（"SOHO办公"、"路由器"、"Wi-Fi"、"高端家庭"）进行检索并打印命中结果，便于验证 ES 向量存储可用性。

---

### 五、Tool Calling 设备查询详解

#### 5.1 工具定义

```java
// DeviceBaseInfoTools
@Tool(description = "")
public DeviceBaseInfo queryDeviceById
```
#### 5.2 工具返回数据结构（DeviceBaseInfo）
| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 设备唯一ID |
| `name` | String | 设备名称 |
| `model` | String | 设备型号 |
| `type` | String | 设备类型（如 ROUTER） |
| `price` | String | 参考价格 |
| `detail` | String | 设备详细描述 |
| `core` | String | 核心功能亮点 |
| `productUrl` | String | 官网跳转链接 |
#### 5.3 Tool Calling 触发流程
```
LLM 基于 RAG 检索的设备上下文进行推理
  │
  ├─ 情况A（需求明确）：遍历候选 → 按匹配规则排序 → 决定调用
  │    queryDeviceById(设备ID1)
  │    queryDeviceById(设备ID2)
  │    queryDeviceById(设备ID3)
  │
  └─ 情况B（需求模糊）：向用户追问使用场景/功能需求 → 收集信息后匹配
```
#### 5.4 Tool 结果缓存与传递（ToolResultUtils）
```
Tool Calling 生命周期内：
  LLM 调用 queryDeviceById(1001) → DeviceBaseInfoTools 返回 DeviceBaseInfo
      │
      ▼ 结果存入 ToolResultUtils（内存 ConcurrentHashMap）
  key: requestId → value: { device-1001: {...JSON...} }
      │
      ▼ 流结束时 transformChatResponse() 检测 finishReason == STOP
  从 ToolResultUtils 取出结果 → 写入 PARAM 事件 → 通过 SSE 发送给前端
      │
      ▼ 发送后 ToolResultUtils.remove(requestId) 清理
```

**缓存特性：**
- 以 `requestId` 为 key，5 分钟 TTL
- 后台守护线程每分钟清理过期条目
- 最大容量 10,000 条，容量溢出时拒绝写入
---
### 六、推荐 System Prompt 核心策略
`SystemConstant.RECOMMEND` 定义了 LLM 的设备推荐策略：

#### 匹配规则（严格按优先级排序）

1. **精确关键词匹配** — 设备"适用场景"中包含用户明确关键词 → 最高优先级
2. **语义相似匹配** — 设备适用场景语义与用户需求相似
3. **设备类型匹配** — 设备类型与用户要求类型一致
4. **其他维度匹配** — 产品分类、核心功能等

#### 两阶段策略

| 阶段 | 触发条件 | 行为 |
|------|----------|------|
| **直接推荐** | 用户需求明确（如"SOHO办公路由器"） | 遍历候选→排序→`queryDeviceById` 查 Top 3→输出推荐 |
| **追问引导** | 用户需求模糊（如"推荐个路由器"） | 追问使用场景、核心功能需求→收集信息后匹配 |

#### 输出格式要求

- 设备名称 + 型号
- 适用场景
- 核心功能亮点
- 参考价格
- 官网跳转链接
- 推荐理由（引用适用场景原文说明匹配依据）

---

### 七、Advisor 拦截器链

在 `SpringAIConfig` 中配置了三条全局默认 Advisor，按顺序执行：

| 顺序 | Advisor | 职责 | 执行阶段 |
|------|---------|------|----------|
| 1 | `SimpleLoggerAdvisor` | DEBUG 级别打印请求/响应日志 | 前后环绕 |
| 2 | `AiRecordOptimizationAdvisor` | 检测意图路由结果，清理路由消息记录 | 后处理（after） |
| 3 | `MessageChatMemoryAdvisor` | 对话记忆管理（窗口 100 条） | 前后环绕 |

**额外 Advisor：** `RecommendBaseAgent` 通过 `advisors()` 方法额外注入 `QuestionAnswerAdvisor`，负责 RAG 向量检索。

> `AiRecordOptimizationAdvisor` 的特殊逻辑：当路由 Agent 返回意图文本（如 "RECOMMEND"）后，该 Advisor 会调用 `DBChatMemoryRepository.optimizationRecord()` 删除最后 2 条路由消息，确保用户看到的是干净的推荐内容，而非路由中间结果。

---

### 八、流式中断控制

```
用户点击 "停止生成" 
  │
  ▼
POST /api/ai/stop/{sessionId}
  │
  ▼
AgentOrchestrator.stop(sessionId)
  │
  ▼ 遍历所有 Agent 调用 stop()
  redisService.deleteObject("chat:generate_status:{sessionId}")
  │
  ▼ 流式管道检测
  takeWhile(response → redisService.getCacheObject(generateStatusKey) == true)
  │
  └─ Redis key 被删除 → takeWhile 返回 false → 流终止
      doOnCancel 钩子触发 → 保存已生成的部分内容到 chatMemory
```
---
### 九、对话记忆持久化（DBChatMemoryRepository）

- **存储表：** `chat_message`（PostgreSQL）
- **conversationId 格式：** `{userId}_{sessionId}` — 区分不同用户和会话
- **消息类型：** `USER` / `ASSISTANT` / `SYSTEM`
- **窗口限制：** `MessageWindowChatMemory` 限制最多 100 条消息
- **Tool Calling 结果不计入记忆** — 避免设备详情 JSON 占用上下文窗口

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
