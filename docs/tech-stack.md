# MetaCraft 技术路线

## 1. 技术栈全景图

```mermaid
graph TB
    subgraph "Frontend - 前端技术栈"
        direction TB
        HOS[HarmonyOS]
        ARKTS[ArkTS / ETS]
        OHPM[OHPM]
        RCP[@kit.RemoteCommunicationKit]
        LV_MARKDOWN[@luvi/lv-markdown-in]
    end

    subgraph "Backend - 后端技术栈"
        direction TB
        JAVA[Java 21]
        SPRING[Spring Boot 3.5.9]
        SECURITY[Spring Security + JWT]
        JPA[Spring Data JPA]
        REACTOR[Project Reactor / SSE]
    end

    subgraph "AI - AI 技术栈"
        direction TB
        LANGCHAIN[LangChain4j 1.11.0-beta19]
        DASHSCOPE[DashScope Community Starter]
        QWEN[Qwen Plus]
        ZHIPU[Zhipu AI / CogView]
    end

    subgraph "Database - 数据技术栈"
        direction TB
        POSTGRES[PostgreSQL 14+]
        FLYWAY[Flyway Migrations]
        FS[File System Storage]
    end

    subgraph "DevOps - 开发运维"
        direction TB
        MAVEN[Maven / mvnw]
        SWAGGER[SpringDoc OpenAPI 3]
        DEVICO[DevEco Studio]
        LOMBOK[Lombok]
    end

    HOS --> ARKTS
    ARKTS --> OHPM
    ARKTS --> RCP
    ARKTS --> LV_MARKDOWN

    JAVA --> SPRING
    SPRING --> SECURITY
    SPRING --> JPA
    SPRING --> REACTOR

    SPRING --> LANGCHAIN
    LANGCHAIN --> DASHSCOPE
    DASHSCOPE --> QWEN
    LANGCHAIN --> ZHIPU

    JPA --> POSTGRES
    SPRING --> FLYWAY
    SPRING --> FS

    JAVA --> MAVEN
    SPRING --> SWAGGER
    HOS --> DEVICO
    JAVA --> LOMBOK

    style LANGCHAIN fill:#9f6,stroke:#333,stroke-width:3px
    style QWEN fill:#f96,stroke:#333,stroke-width:2px
    style ZHIPU fill:#69f,stroke:#333,stroke-width:2px
    style SPRING fill:#6f9,stroke:#333,stroke-width:2px
```

## 2. Spring Boot 架构分层

```mermaid
graph LR
    subgraph "Spring Boot Application"
        CONFIG[@Configuration<br/>配置层]
        CONTROLLER[@RestController<br/>控制器层]
        SERVICE[@Service<br/>服务层]
        REPOSITORY[@Repository<br/>数据访问层]
        ENTITY[@Entity<br/>实体层]
    end

    subgraph "Spring Security"
        JWT[JwtTokenProvider]
        FILTER[JwtAuthenticationFilter]
        CONFIG_SEC[SecurityConfig]
    end

    subgraph "LangChain4j"
        AI_SERVICE[@AiService<br/>AI服务接口]
        TOOL[@Tool<br/>工具方法]
    end

    subgraph "Reactive"
        FLUX[Flux<SSE>]
        MONO[Mono<T>]
        SCHEDULER[Schedulers]
    end

    CONTROLLER --> SERVICE
    SERVICE --> REPOSITORY
    REPOSITORY --> ENTITY

    CONFIG_SEC --> FILTER
    FILTER --> JWT

    SERVICE --> AI_SERVICE
    AI_SERVICE --> TOOL

    CONTROLLER --> FLUX
    SERVICE --> MONO
    FLUX --> SCHEDULER

    style AI_SERVICE fill:#f96,stroke:#333,stroke-width:2px
    style TOOL fill:#f96,stroke:#333,stroke-width:2px
```

## 3. LangChain4j 集成架构

```mermaid
graph TB
    subgraph "LangChain4j Components"
        AI_SERVICE[@AiService<br/>接口定义]
        IMPL[自动实现<br/>运行时生成]
        SYSTEM[@SystemMessage<br/>系统提示词]
        USER[@UserMessage<br/>用户消息]
        VARIABLE[@V<br/>变量注入]
        P_TOOL[@Tool<br/>工具调用]
        STREAMING[Flux Streaming<br/>流式输出]
    end

    subgraph "Prompt Templates"
        P1[prompts/intent.txt]
        P2[prompts/chat.txt]
        P3[prompts/gen-plan.txt]
        P4[prompts/gen-code.txt]
        P5[prompts/gen-app-info.txt]
    end

    subgraph "AI Providers"
        DASH[DashScope<br/>Qwen Plus]
        ZHIPU[Zhipu AI<br/>CogView]
    end

    AI_SERVICE --> IMPL
    IMPL --> SYSTEM
    IMPL --> USER
    IMPL --> VARIABLE
    IMPL --> P_TOOL
    IMPL --> STREAMING

    SYSTEM --> P1
    SYSTEM --> P2
    SYSTEM --> P3
    SYSTEM --> P4
    SYSTEM --> P5

    IMPL --> DASH
    P_TOOL --> ZHIPU

    style AI_SERVICE fill:#9f6,stroke:#333,stroke-width:2px
    style P_TOOL fill:#f69,stroke:#333,stroke-width:2px
    style STREAMING fill:#69f,stroke:#333,stroke-width:2px
```

## 4. 数据库迁移路线

```mermaid
graph LR
    subgraph "Flyway Migrations"
        V1[V1__init_base.sql<br/>基础函数]
        V2[V2__create_user_tables.sql<br/>用户表]
        V3[V3__create_apps_and_versions_table.sql<br/>应用与版本]
        V4[V4__create_chat_sessions_table.sql<br/>会话表]
        V5[V5__create_chat_messages_table.sql<br/>消息表]
        V6[V6__add_logo_to_apps.sql<br/>Logo字段]
    end

    subgraph "Schema Evolution"
        S1[(Phase 1<br/>用户系统)]
        S2[(Phase 2<br/>应用管理)]
        S3[(Phase 3<br/>对话系统)]
        S4[(Phase 4<br/>Logo功能)]
    end

    V1 --> S1
    V2 --> S1
    V3 --> S2
    V4 --> S3
    V5 --> S3
    V6 --> S4

    V1 -.-> V2
    V2 -.-> V3
    V3 -.-> V4
    V4 -.-> V5
    V5 -.-> V6

    style V3 fill:#f96,stroke:#333,stroke-width:2px
    style V5 fill:#69f,stroke:#333,stroke-width:2px
```

## 5. HarmonyOS 前端架构

```mermaid
graph TB
    subgraph "AppScope"
        BUNDLE[app.json5<br/>Bundle配置]
        ICONS[resources/base/media<br/>图标资源]
    end

    subgraph "Entry Module"
        ABILITY[EntryAbility<br/>应用入口]
        PAGES[pages/<br/>页面容器]
        COMPONENTS[components/<br/>可复用组件]
        API[api/<br/>API封装]
        MODEL[model/<br/>数据模型]
        UTILS[utils/<br/>工具类]
    end

    subgraph "Ohos_agcit Modules"
        LOGIN[aggregated_login<br/>登录模块]
        COLLECT[collect_personal_info<br/>信息收集]
        SETTING[app_setting<br/>设置模块]
    end

    subgraph "Network Layer"
        HTTP[HttpManager<br/>HTTP管理器]
        SSE[SSEClient<br/>SSE客户端]
        INTERCEPT[Interceptors<br/>拦截器]
        CONFIG[ApiConfig<br/>API配置]
    end

    subgraph "UI Components"
        INDEX[Index.ets<br/>主页]
        CHAT[ChatPanel.ets<br/>聊天面板]
        SIDEBAR[SessionList.ets<br/>会话列表]
        PREVIEW[Preview.ets<br/>预览页]
        CARD[AppPreviewCard.ets<br/>应用卡片]
    end

    BUNDLE --> ABILITY
    ABILITY --> PAGES
    PAGES --> COMPONENTS
    PAGES --> API
    API --> MODEL
    API --> UTILS

    LOGIN --> ABILITY
    COLLECT --> ABILITY
    SETTING --> ABILITY

    UTILS --> HTTP
    UTILS --> SSE
    HTTP --> INTERCEPT
    HTTP --> CONFIG
    SSE --> INTERCEPT

    COMPONENTS --> INDEX
    COMPONENTS --> CHAT
    COMPONENTS --> SIDEBAR
    COMPONENTS --> PREVIEW
    COMPONENTS --> CARD

    style HTTP fill:#f96,stroke:#333,stroke-width:2px
    style SSE fill:#69f,stroke:#333,stroke-width:2px
    style CHAT fill:#9f6,stroke:#333,stroke-width:2px
```

## 6. 前端网络层设计

```mermaid
sequenceDiagram
    participant Page as Page Component
    participant API as API Module
    participant Http as HttpManager
    participant Interceptor as Interceptors
    participant RCP as rcp.Session
    participant Server as Backend Server

    rect rgb(200, 220, 240)
        Note over Page,Server: HTTP 请求流程
        Page->>API: authApi.login()
        API->>Http: post("/auth/login", body)
        Http->>Interceptor: requestInterceptor
        Interceptor->>Interceptor: 添加通用 headers
        Interceptor-->>Http: modified request
        Http->>RCP: session.post()
        RCP->>Server: HTTP POST
        Server-->>RCP: Response
        RCP-->>Http: Response
        Http->>Interceptor: responseInterceptor
        Interceptor->>Interceptor: 处理 401/403
        Interceptor-->>Http: processed response
        Http-->>API: data
        API-->>Page: AuthTokenVO
    end

    rect rgb(220, 240, 200)
        Note over Page,Server: SSE 流程
        Page->>API: aiChatApi.sendStreamMessage()
        API->>Http: createSSE(url, body)
        Http->>SSE: SSEClient(url, listener)
        SSE->>RCP: rcp.createHttpConnection()
        SSE->>Interceptor: AuthInterceptor.addAuth()
        RCP->>Server: SSE Connection
        Server-->>SSE: text/event-stream
        loop Stream Events
            SSE->>SSE: parseEvent(chunk)
            SSE->>Page: listener.onIntent()
            SSE->>Page: listener.onMessage()
            SSE->>Page: listener.onPlan()
            SSE->>Page: listener.onAppGenerated()
        end
    end
```

## 7. 安全架构

```mermaid
graph TB
    subgraph "Authentication - 认证"
        Creds[用户凭证<br/>email + password]
        BCrypt[BCrypt 哈希]
        JWT[JWT Token<br/>HMAC-SHA256]
        TokenHeader[Authorization Header<br/>Bearer {token}]
    end

    subgraph "Authorization - 授权"
        SecurityConfig[SecurityConfig<br/>安全配置]
        PublicEndpoints[/api/auth/**<br/>/api/preview/**]
        ProtectedEndpoints[/api/agent/**<br/>/api/user/**<br/>/api/sessions/**]
        ManualValidation[AuthUtils<br/>手动验证]
    end

    subgraph "Storage Security - 存储安全"
        PathNormalize[路径归一化<br/>normalize()]
        PathValidate[路径验证<br/>startsWith()]
        UUIDAccess[UUID 访问控制<br/>防止ID枚举]
    end

    Creds --> BCrypt
    BCrypt --> JWT
    JWT --> TokenHeader

    TokenHeader --> SecurityConfig
    SecurityConfig --> PublicEndpoints
    SecurityConfig --> ProtectedEndpoints
    ProtectedEndpoints --> ManualValidation

    SecurityConfig --> PathNormalize
    PathNormalize --> PathValidate
    PathValidate --> UUIDAccess

    style JWT fill:#f96,stroke:#333,stroke-width:2px
    style ManualValidation fill:#f69,stroke:#333,stroke-width:2px
    style UUIDAccess fill:#69f,stroke:#333,stroke-width:2px
```

## 8. 技术选型说明

### 后端技术栈

| 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|----------|
| Java | 21 | 基础语言 | LTS 版本，虚拟线程支持 |
| Spring Boot | 3.5.9 | 应用框架 | 成熟生态，自动配置 |
| Spring Security | - | 安全框架 | 标准 Java 安全解决方案 |
| JWT | java-jwt 4.4.0 | Token 认证 | 无状态认证 |
| Spring Data JPA | - | ORM | 标准 JPA 实现 |
| PostgreSQL | 14+ | 数据库 | 开源关系型数据库，支持 JSON |
| Flyway | - | 数据库迁移 | 版本化数据库管理 |
| Project Reactor | - | 响应式流 | SSE 流式输出支持 |
| LangChain4j | 1.11.0-beta19 | AI 集成 | Java AI 编排框架 |
| DashScope | - | AI 模型 | 通义千问 API |
| SpringDoc | 2.8.14 | API 文档 | OpenAPI 3.0 支持 |
| Lombok | - | 代码简化 | 减少样板代码 |

### 前端技术栈

| 技术 | 用途 | 选型理由 |
|------|------|----------|
| HarmonyOS | 操作系统 | 华为自研操作系统 |
| ArkTS | 开发语言 | TypeScript 变体，强类型 |
| ETS | UI 框架 | 声明式 UI |
| @kit.RemoteCommunicationKit | 网络通信 | HarmonyOS 原生网络库 |
| @luvi/lv-markdown-in | Markdown 渲染 | 第三方 Markdown 组件 |

### AI 技术栈

| 技术 | 用途 | 选型理由 |
|------|------|----------|
| LangChain4j | AI 编排 | Java 领域标准 AI 框架 |
| @AiService | AI 服务 | 声明式 AI 服务定义 |
| @Tool | 工具调用 | AI 函数调用支持 |
| DashScope Qwen Plus | 聊天模型 | 通义千问，支持长上下文 |
| Zhipu CogView | 图像生成 | Logo 生成 |

### 技术趋势

```mermaid
graph LR
    subgraph "Current - 当前"
        MONO[单体应用]
        SYNC[同步请求]
        STATEFUL[有状态]
    end

    subgraph "Evolution - 演进方向"
        MICRO[微服务化]
        ASYNC[异步消息]
        SERVERLESS[无状态]
        CACHE[缓存层]
        QUEUE[消息队列]
    end

    subgraph "Future - 未来可能"
        EDGE[边缘部署]
        REALTIME[实时协作]
        MULTI_MODEL[多模态 AI]
    end

    MONO -.-> MICRO
    SYNC -.-> ASYNC
    STATEFUL -.-> SERVERLESS

    MICRO --> QUEUE
    ASYNC --> CACHE
    SERVERLESS --> CACHE

    CACHE -.-> EDGE
    QUEUE -.-> REALTIME
    MICRO -.-> MULTI_MODEL

    style MULTI_MODEL fill:#f96,stroke:#333,stroke-width:2px
    style REALTIME fill:#69f,stroke:#333,stroke-width:2px
```

## 技术债务与改进方向

1. **缓存层**: 引入 Redis 缓存热点数据（用户信息、应用元数据）
2. **异步处理**: 应用生成耗时操作使用消息队列解耦
3. **限流熔断**: 添加 Resilience4j 保护 API
4. **监控告警**: 集成 Prometheus + Grafana
5. **日志聚合**: 使用 ELK/Loki 集中式日志
6. **前端优化**: 虚拟滚动、懒加载优化长列表性能
