# MetaCraft 系统架构

## 1. 整体系统架构

```mermaid
graph TB
    subgraph "Client Layer - 客户端层"
        HOS[HarmonyOS App<br/>ArkTS/ETS]
        WEB[Web Browser<br/>Preview Apps]
    end

    subgraph "API Gateway Layer - API 网关层"
        SECURITY[Spring Security<br/>JWT Filter]
        MVC[WebMvcConfig<br/>API Prefix /api]
    end

    subgraph "Application Layer - 应用层"
        UNIFIED[UnifiedController<br/>POST /ai/agent/unified]
        AUTH[AuthController<br/>/auth/*]
        USER[UserController<br/>/user/*]
        SESSION[ChatSessionController<br/>/ai/sessions/*]
        PREVIEW[PreviewController<br/>/preview/{uuid}]
    end

    subgraph "Domain Layer - 领域层"
        ORCHESTRATOR[UnifiedOrchestrator<br/>统一编排器]
        INTENT[IntentAnalyzer<br/>意图识别]
        PIPELINE[Pipeline Services<br/>流水线服务]
        AGENTS[AI Agents<br/>ChatAgent/CodeGenerator<br/>PlanGenerator/AppInfoExtractor]
    end

    subgraph "Infrastructure Layer - 基础设施层"
        LANGCHAIN[LangChain4j<br/>@AiService/@Tool]
        AIProvider[AI Providers<br/>DashScope Qwen/Zhipu]
        STORAGE[StorageService<br/>文件存储]
        REPOS[(Repositories<br/>JPA)]
    end

    subgraph "Data Layer - 数据层"
        POSTGRES[(PostgreSQL<br/>users/apps/app_versions<br/>chat_sessions/chat_messages)]
        FILES[(File System<br/>apps/data/{appId}/v{version}/)]
    end

    HOS -->|HTTPS + JWT| SECURITY
    WEB -->|HTTPS| PREVIEW
    SECURITY --> MVC
    MVC --> UNIFIED
    MVC --> AUTH
    MVC --> USER
    MVC --> SESSION
    MVC --> PREVIEW

    UNIFIED --> ORCHESTRATOR
    AUTH --> USER
    SESSION --> ORCHESTRATOR

    ORCHESTRATOR --> INTENT
    INTENT --> PIPELINE
    PIPELINE --> AGENTS
    AGENTS --> LANGCHAIN
    LANGCHAIN --> AIProvider

    ORCHESTRATOR --> REPOS
    AGENTS --> STORAGE
    STORAGE --> FILES
    REPOS --> POSTGRES

    style HOS fill:#f9f,stroke:#333,stroke-width:2px
    style WEB fill:#bbf,stroke:#333,stroke-width:2px
    style LANGCHAIN fill:#bfb,stroke:#333,stroke-width:2px
    style POSTGRES fill:#ffd,stroke:#333,stroke-width:2px
```

## 2. 意图路由架构

```mermaid
graph LR
    subgraph "UnifiedOrchestrator"
        IN[User Message<br/>用户消息]
        PREP[Prepare & Save<br/>准备并保存]
        INTENT[IntentAnalyzer<br/>意图分析]
    end

    subgraph "Pipeline Services"
        CHAT[ChatPipelineService<br/>对话流水线]
        GEN[AppGenPipelineService<br/>应用生成流水线]
        EDIT[AppEditPipelineService<br/>应用编辑流水线]
    end

    subgraph "AI Agents"
        CHAT_AGENT[ChatAgent<br/>聊天代理]
        PLAN[PlanGenerator<br/>方案生成]
        INFO[AppInfoExtractor<br/>应用信息提取]
        CODE[CodeGenerator<br/>代码生成]
    end

    IN --> PREP
    PREP --> INTENT

    INTENT -->|CHAT| CHAT
    INTENT -->|GEN| GEN
    INTENT -->|EDIT| EDIT

    CHAT --> CHAT_AGENT
    GEN --> PLAN
    GEN --> INFO
    GEN --> CODE

    style INTENT fill:#faa,stroke:#333,stroke-width:3px
    style CHAT fill:#afa,stroke:#333,stroke-width:2px
    style GEN fill:#aaf,stroke:#333,stroke-width:2px
    style EDIT fill:#ffa,stroke:#333,stroke-width:2px
```

## 3. 数据模型架构

```mermaid
erDiagram
    USERS ||--o{ CHAT_SESSIONS : creates
    USERS ||--o{ CHAT_MESSAGES : sends
    USERS ||--o{ APPS : owns

    APPS ||--|| APP_VERSIONS : has_current
    APPS ||--o{ APP_VERSIONS : contains
    APPS ||--o{ CHAT_MESSAGES : references

    CHAT_SESSIONS ||--o{ CHAT_MESSAGES : contains

    USERS {
        bigint id PK
        text email UK
        text name
        text password_hash
        text avatar_base64
        text bio
        timestamptz created_at
        timestamptz updated_at
    }

    APPS {
        bigint id PK
        varchar uuid UK
        bigint user_id FK
        varchar name
        text description
        boolean is_public
        bigint current_version_id FK
        varchar logo_path
        timestamptz created_at
        timestamptz updated_at
    }

    APP_VERSIONS {
        bigint id PK
        bigint app_id FK
        int version_number
        varchar storage_path
        text change_log
        timestamptz created_at
    }

    CHAT_SESSIONS {
        varchar session_id PK
        bigint user_id FK
        bigint app_id FK
        varchar title
        timestamptz created_at
        timestamptz updated_at
    }

    CHAT_MESSAGES {
        bigint id PK
        bigint user_id FK
        varchar session_id FK
        varchar role
        text content
        varchar type
        bigint related_app_id FK
        bigint related_version_id FK
        timestamptz created_at
    }
```

## 4. 模块依赖关系

```mermaid
graph TB
    subgraph "controllers"
        UnifiedController
        AuthController
        UserController
        ChatSessionController
        PreviewController
    end

    subgraph "services"
        UnifiedOrchestrator
        ChatSessionService
        ChatMessageService
        AppService
        ImageService
        UserService
    end

    subgraph "pipelines"
        ChatPipelineService
        AppGenPipelineService
        AppEditPipelineService
    end

    subgraph "agents"
        IntentAnalyzer
        ChatAgent
        PlanGenerator
        AppInfoExtractor
        CodeGenerator
    end

    subgraph "security"
        JwtTokenProvider
        JwtAuthenticationFilter
        AuthUtils
    end

    subgraph "storage"
        StorageService
    end

    subgraph "repositories"
        UserRepository
        AppRepository
        AppVersionRepository
        ChatSessionRepository
        ChatMessageRepository
    end

    UnifiedController --> UnifiedOrchestrator
    UnifiedController --> AuthUtils

    AuthController --> UserService
    UserController --> UserService

    ChatSessionController --> ChatSessionService
    ChatSessionController --> ChatMessageService

    PreviewController --> AppService
    PreviewController --> StorageService

    UnifiedOrchestrator --> ChatSessionService
    UnifiedOrchestrator --> ChatMessageService
    UnifiedOrchestrator --> IntentAnalyzer
    UnifiedOrchestrator --> ChatPipelineService
    UnifiedOrchestrator --> AppGenPipelineService
    UnifiedOrchestrator --> AppEditPipelineService

    ChatPipelineService --> ChatAgent
    AppGenPipelineService --> ChatAgent
    AppGenPipelineService --> PlanGenerator
    AppGenPipelineService --> AppInfoExtractor
    AppGenPipelineService --> CodeGenerator
    AppGenPipelineService --> AppService
    AppGenPipelineService --> ImageService
    AppGenPipelineService --> ChatMessageService

    AppEditPipelineService --> CodeGenerator

    UserService --> UserRepository
    AppService --> AppRepository
    AppService --> AppVersionRepository
    AppService --> StorageService
    ImageService --> StorageService
    ChatSessionService --> ChatSessionRepository
    ChatMessageService --> ChatMessageRepository

    JwtAuthenticationFilter --> JwtTokenProvider

    style UnifiedOrchestrator fill:#f96,stroke:#333,stroke-width:3px
    style IntentAnalyzer fill:#f96,stroke:#333,stroke-width:2px
```

## 5. 文件存储结构

```mermaid
graph TB
    ROOT[apps/data/]

    subgraph "App 1"
        APP1[apps/data/1/]
        V1[v1/]
        V2[v2/]
        V3[v3/]
        INDEX1[index.html]
        JS1[app.js]
        LOGO1[logo.png]

        APP1 --> V1
        APP1 --> V2
        APP1 --> V3
        V1 --> INDEX1
        V1 --> JS1
        V2 --> INDEX1
        V2 --> JS1
        V3 --> INDEX1
        V3 --> JS1
        APP1 --> LOGO1
    end

    subgraph "App 2"
        APP2[apps/data/2/]
        V1_2[v1/]
        INDEX2[index.html]
        JS2[app.js]
        LOGO2[logo.jpg]

        APP2 --> V1_2
        V1_2 --> INDEX2
        V1_2 --> JS2
        APP2 --> LOGO2
    end

    ROOT --> APP1
    ROOT --> APP2

    style ROOT fill:#ffd,stroke:#333,stroke-width:2px
    style APP1 fill:#afa,stroke:#333,stroke-width:2px
    style APP2 fill:#aaf,stroke:#333,stroke-width:2px
```

## 6. SSE 事件流架构

```mermaid
graph LR
    subgraph "Server"
        CONTROLLER[UnifiedController]
        ORCHESTRATOR[UnifiedOrchestrator]
        PIPELINE[PipelineService]
        SSE[ServerSentEvent<br/>Publisher]
    end

    subgraph "Events"
        INTENT[intent<br/>意图识别结果]
        MESSAGE[message<br/>文本流片段]
        PLAN[plan<br/>生成方案片段]
        APP_INFO[app_info<br/>应用信息]
        LOGO[logo_generated<br/>logo生成完成]
        APP_GEN[app_generated<br/>应用生成完成]
        DONE[done<br/>流结束]
        ERROR[error<br/>错误事件]
    end

    subgraph "Client"
        SSECLIENT[SSEClient<br/>HarmonyOS]
        LISTENER[SSEListener<br/>Event Handlers]
        UI[ChatPanel<br/>PreviewCard]
    end

    CONTROLLER --> ORCHESTRATOR
    ORCHESTRATOR --> PIPELINE
    PIPELINE --> SSE

    SSE --> INTENT
    SSE --> MESSAGE
    SSE --> PLAN
    SSE --> APP_INFO
    SSE --> LOGO
    SSE --> APP_GEN
    SSE --> DONE
    SSE --> ERROR

    INTENT --> SSECLIENT
    MESSAGE --> SSECLIENT
    PLAN --> SSECLIENT
    APP_INFO --> SSECLIENT
    LOGO --> SSECLIENT
    APP_GEN --> SSECLIENT
    DONE --> SSECLIENT
    ERROR --> SSECLIENT

    SSECLIENT --> LISTENER
    LISTENER --> UI

    style SSE fill:#f96,stroke:#333,stroke-width:2px
    style SSECLIENT fill:#6f9,stroke:#333,stroke-width:2px
```

## 架构说明

### 分层架构

1. **Client Layer (客户端层)**
   - HarmonyOS 原生应用（ArkTS/ETS）
   - Web 浏览器（用于预览生成的应用）

2. **API Gateway Layer (API 网关层)**
   - Spring Security + JWT 认证
   - 统一 API 前缀 `/api`
   - 跨域配置

3. **Application Layer (应用层)**
   - RESTful API 控制器
   - 统一入口 `POST /ai/agent/unified`

4. **Domain Layer (领域层)**
   - 统一编排器协调意图识别和流水线
   - AI Agents 封装 LangChain4j 服务

5. **Infrastructure Layer (基础设施层)**
   - LangChain4j AI 集成
   - 存储服务
   - 数据仓库

6. **Data Layer (数据层)**
   - PostgreSQL 数据库
   - 文件系统（应用代码）

### 核心设计模式

1. **统一编排模式**: `UnifiedOrchestrator` 作为中心协调器
2. **意图路由模式**: 根据用户意图动态选择处理流水线
3. **流水线模式**: 每种意图对应独立的流水线服务
4. **SSE 流式模式**: 实时推送 AI 生成进度
