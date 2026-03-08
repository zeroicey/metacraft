# MetaCraft 时序用例图

## 1. 用户认证流程

```mermaid
sequenceDiagram
    autonumber
    participant HOS as HarmonyOS App
    participant Auth as AuthController
    participant User as UserService
    participant Jwt as JwtTokenProvider
    participant DB as PostgreSQL

    rect rgb(200, 220, 240)
        Note over HOS,DB: 用户注册
        HOS->>Auth: POST /api/auth/register<br/>{email, password, name}
        Auth->>User: register(dto)
        User->>User: BCrypt.hashpw(password)
        User->>DB: INSERT INTO users
        DB-->>User: UserEntity
        User-->>Auth: UserVO
        Auth-->>HOS: 200 OK {user}
    end

    rect rgb(220, 240, 200)
        Note over HOS,DB: 用户登录
        HOS->>Auth: POST /api/auth/login<br/>{email, password}
        Auth->>User: login(dto)
        User->>DB: SELECT * FROM users<br/>WHERE email = ?
        DB-->>User: UserEntity
        User->>User: BCrypt.checkpw()
        User->>Jwt: generateToken(user)
        Jwt-->>User: JWT
        User-->>Auth: AuthTokenVO
        Auth-->>HOS: 200 OK {token, user}
    end
```

## 2. 统一 AI 对话流程（CHAT 意图）

```mermaid
sequenceDiagram
    autonumber
    participant Client as HarmonyOS App
    participant Controller as UnifiedController
    participant Orchestrator as UnifiedOrchestrator
    participant Intent as IntentAnalyzer
    participant Pipeline as ChatPipelineService
    participant Agent as ChatAgent
    participant LangChain as LangChain4j
    participant AI as DashScope Qwen
    participant DB as PostgreSQL

    Client->>Controller: POST /api/agent/unified<br/>{message, sessionId}<br/>Authorization: Bearer {token}
    Controller->>Controller: validate JWT
    Controller->>Orchestrator: handleRequest(request, userId)

    rect rgb(240, 230, 200)
        Note over Orchestrator,DB: 准备阶段
        Orchestrator->>Orchestrator: resolveOrCreateSession()
        Orchestrator->>DB: INSERT INTO chat_messages<br/>(role: user, content)
        DB-->>Orchestrator: saved
    end

    rect rgb(220, 240, 200)
        Note over Orchestrator,AI: 意图识别与处理
        Orchestrator->>Intent: analyze(message)
        Intent->>LangChain: chat(intent prompt)
        LangChain->>AI: API Request
        AI-->>LangChain: "CHAT"
        LangChain-->>Intent: Intent.CHAT
        Intent-->>Orchestrator: Intent.CHAT

        Orchestrator->>Pipeline: execute(message, userId, sessionId)
        Pipeline->>Agent: chat(message)
        Agent->>LangChain: stream chat
        LangChain->>AI: Stream Request

        loop 流式响应
            AI-->>LangChain: chunk
            LangChain-->>Agent: chunk
            Agent-->>Pipeline: chunk
            Pipeline-->>Orchestrator: SSE(message)
            Orchestrator-->>Client: event: message<br/>data: {chunk}
        end
    end

    rect rgb(240, 200, 220)
        Note over Pipeline,DB: 保存助手回复
        Pipeline->>DB: INSERT INTO chat_messages<br/>(role: assistant, content)
        DB-->>Pipeline: saved
    end

    Orchestrator-->>Client: event: done
```

## 3. 应用生成流程（GEN 意图）

```mermaid
sequenceDiagram
    autonumber
    participant Client as HarmonyOS App
    participant Controller as UnifiedController
    participant Orchestrator as UnifiedOrchestrator
    participant Intent as IntentAnalyzer
    participant Pipeline as AppGenPipelineService
    participant ChatAgent as ChatAgent
    participant PlanGen as PlanGenerator
    participant InfoExt as AppInfoExtractor
    participant CodeGen as CodeGenerator
    participant AppSvc as AppService
    participant ImgSvc as ImageService
    participant Storage as StorageService
    participant DB as PostgreSQL

    Client->>Controller: POST /api/agent/unified<br/>"生成一个待办事项应用"
    Controller->>Orchestrator: handleRequest(request, userId)

    rect rgb(240, 230, 200)
        Note over Orchestrator,DB: 准备阶段
        Orchestrator->>DB: INSERT chat_messages (user)
        Orchestrator->>Intent: analyze()
        Intent-->>Orchestrator: Intent.GEN
        Orchestrator-->>Client: event: intent<br/>data: "GEN"
    end

    rect rgb(220, 240, 200)
        Note over Pipeline,ChatAgent: 生成前对话
        Pipeline->>ChatAgent: chatBeforeGen()
        loop 流式响应
            ChatAgent-->>Pipeline: chunk
            Pipeline-->>Client: event: message
        end
    end

    rect rgb(240, 200, 220)
        Note over Pipeline,PlanGen: 生成方案
        Pipeline->>PlanGen: generatePlan()
        loop 流式响应
            PlanGen-->>Pipeline: chunk
            Pipeline-->>Client: event: plan
        end
    end

    rect rgb(200, 220, 240)
        Note over Pipeline,AppSvc: 提取应用信息并创建
        Pipeline->>InfoExt: extract(message)
        InfoExt-->>Pipeline: AppInfoDTO{name, description}
        Pipeline->>AppSvc: createApp()
        AppSvc->>DB: INSERT INTO apps
        DB-->>AppSvc: AppEntity{id, uuid}
        AppSvc-->>Pipeline: AppEntity
        Pipeline-->>Client: event: app_info
    end

    rect rgb(240, 220, 180)
        Note over Pipeline,ImgSvc: 生成 Logo
        Pipeline->>ImgSvc: generateLogoAndSave(app, logoUuid)
        ImgSvc->>ImgSvc: call Zhipu API
        ImgSvc->>Storage: saveLogo(uuid, imageBytes)
        Storage-->>ImgSvc: path
        ImgSvc->>DB: UPDATE apps SET logo_path
        Pipeline-->>Client: event: logo_generated
    end

    rect rgb(220, 180, 240)
        Note over Pipeline,Storage: 生成代码
        Pipeline->>CodeGen: generateCode()
        CodeGen-->>Pipeline: AppCodeDTO{htmlCode, jsCode}
        Pipeline->>AppSvc: createVersion(appId, code)
        AppSvc->>Storage: saveFile(apps/{id}/v{version}/index.html)
        AppSvc->>DB: INSERT INTO app_versions
        AppSvc->>DB: UPDATE apps SET current_version_id
        DB-->>AppSvc: AppVersionEntity
        AppSvc-->>Pipeline: version
        Pipeline-->>Client: event: app_generated<br/>data: {uuid, previewUrl}
    end

    rect rgb(240, 200, 200)
        Note over Pipeline,DB: 保存助手消息
        Pipeline->>DB: INSERT INTO chat_messages<br/>(role: assistant, type: app,<br/>related_app_id, related_version_id)
    end

    Pipeline-->>Client: event: done
```

## 4. 应用预览流程

```mermaid
sequenceDiagram
    autonumber
    participant Browser as Web Browser
    participant Preview as PreviewController
    participant AppSvc as AppService
    participant Storage as StorageService
    participant DB as PostgreSQL

    rect rgb(200, 220, 240)
        Note over Browser,DB: 预览最新版本
        Browser->>Preview: GET /api/preview/{uuid}
        Preview->>AppSvc: getAppByUuid(uuid)
        AppSvc->>DB: SELECT * FROM apps<br/>WHERE uuid = ?
        DB-->>AppSvc: AppEntity
        AppSvc->>DB: SELECT * FROM app_versions<br/>WHERE id = current_version_id
        DB-->>AppSvc: AppVersionEntity
        AppSvc->>Storage: readFile(storage_path)
        Storage-->>AppSvc: htmlContent
        AppSvc-->>Preview: HTML
        Preview-->>Browser: 200 OK text/html
    end

    rect rgb(220, 240, 200)
        Note over Browser,DB: 预览指定版本
        Browser->>Preview: GET /api/preview/{uuid}/v/2
        Preview->>AppSvc: getAppByUuid(uuid)
        AppSvc->>DB: SELECT * FROM apps<br/>WHERE uuid = ?
        DB-->>AppSvc: AppEntity
        Preview->>DB: SELECT * FROM app_versions<br/>WHERE app_id = ? AND version_number = 2
        DB-->>Preview: AppVersionEntity
        Preview->>Storage: readFile(storage_path)
        Storage-->>Preview: htmlContent
        Preview-->>Browser: 200 OK text/html
    end
```

## 5. 会话管理流程

```mermaid
sequenceDiagram
    autonumber
    participant Client as HarmonyOS App
    participant SessionCtl as ChatSessionController
    participant SessionSvc as ChatSessionService
    participant MsgSvc as ChatMessageService
    participant DB as PostgreSQL

    rect rgb(200, 220, 240)
        Note over Client,DB: 获取会话列表
        Client->>SessionCtl: GET /api/ai/sessions
        SessionCtl->>SessionSvc: getSessions(userId)
        SessionSvc->>DB: SELECT * FROM chat_sessions<br/>WHERE user_id = ?<br/>ORDER BY updated_at DESC
        DB-->>SessionSvc: List<ChatSessionEntity>
        SessionSvc-->>SessionCtl: List<ChatSessionVO>
        SessionCtl-->>Client: 200 OK [{sessionId, title, ...}]
    end

    rect rgb(220, 240, 200)
        Note over Client,DB: 创建会话
        Client->>SessionCtl: POST /api/ai/sessions<br/>{title}
        SessionCtl->>SessionSvc: createSession(userId, dto)
        SessionSvc->>DB: INSERT INTO chat_sessions<br/>(session_id, user_id, title)
        DB-->>SessionSvc: ChatSessionEntity
        SessionSvc-->>SessionCtl: ChatSessionVO
        SessionCtl-->>Client: 201 Created {sessionId, title}
    end

    rect rgb(240, 220, 180)
        Note over Client,DB: 获取会话消息
        Client->>SessionCtl: GET /api/ai/sessions/{id}/messages
        SessionCtl->>MsgSvc: getMessages(userId, sessionId)
        MsgSvc->>DB: SELECT * FROM chat_messages<br/>WHERE session_id = ?<br/>ORDER BY created_at ASC
        DB-->>MsgSvc: List<ChatMessageEntity>
        MsgSvc-->>SessionCtl: List<ChatMessageVO>
        SessionCtl-->>Client: 200 OK [{role, content, type, ...}]
    end

    rect rgb(240, 200, 220)
        Note over Client,DB: 删除会话
        Client->>SessionCtl: DELETE /api/ai/sessions/{id}
        SessionCtl->>SessionSvc: deleteSession(userId, sessionId)
        SessionSvc->>DB: DELETE FROM chat_sessions<br/>WHERE session_id = ?
        Note right of DB: CASCADE delete chat_messages
        DB-->>SessionSvc: deleted
        SessionCtl-->>Client: 204 No Content
    end
```

## 6. SSE 连接错误处理

```mermaid
sequenceDiagram
    autonumber
    participant Client as HarmonyOS App
    participant SSEClient as SSEClient
    participant Controller as UnifiedController
    participant Orchestrator as UnifiedOrchestrator
    participant Pipeline as PipelineService

    Client->>SSEClient: sendStreamMessage()
    SSEClient->>Controller: POST /api/agent/unified

    rect rgb(240, 200, 200)
        Note over Controller,Pipeline: 正常流程
        Controller->>Orchestrator: handleRequest()
        Orchestrator->>Pipeline: execute()
        Pipeline-->>SSEClient: event: message
        Pipeline-->>SSEClient: event: plan
        Pipeline-->>SSEClient: event: app_generated
        Pipeline-->>SSEClient: event: done
    end

    rect rgb(240, 180, 180)
        Note over Controller,Pipeline: 错误处理
        Controller->>Orchestrator: handleRequest()
        Orchestrator->>Pipeline: execute()
        Pipeline--xOrchestrator: Exception
        Orchestrator-->>SSEClient: event: error<br/>data: {message}
        SSEClient-->>Client: onError(message)
        Client->>Client: showErrorDialog()
    end

    rect rgb(220, 220, 240)
        Note over Client,SSEClient: 客户端取消
        Client->>SSEClient: cancel()
        SSEClient->>SSEClient: session.close()
        Note right of SSEClient: RCP error 1007900992<br/>(Request canceled)
    end
```

## 7. LangChain4j @Tool 调用流程

```mermaid
sequenceDiagram
    autonumber
    participant Agent as AI Agent
    participant LangChain as LangChain4j Runtime
    participant Tool as @Tool Method
    participant Service as AppService
    participant DB as Database

    rect rgb(220, 240, 200)
        Note over Agent,DB: AI 自动调用工具
        Agent->>LangChain: generateApp(prompt)
        LangChain->>LangChain: parse response<br/>detect @Tool invocation
        LangChain->>Tool: saveApp(name, description, code, userId)

        Tool->>Service: createApp(userId, name, description)
        Service->>DB: INSERT INTO apps
        DB-->>Service: AppEntity{id, uuid}
        Service->>Service: createVersion(appId, code)
        Service-->>Tool: "保存成功: {uuid}"

        Tool-->>LangChain: return value
        LangChain->>Agent: tool result
        Agent->>LangChain: continue with tool result
        LangChain-->>Agent: final response
    end
```

## 用例说明

### 核心用例

| 用例 | 入口 | 意图 | 流水线 | 主要事件 |
|------|------|------|--------|----------|
| 普通对话 | POST /api/agent/unified | CHAT | ChatPipelineService | intent, message, done |
| 应用生成 | POST /api/agent/unified | GEN | AppGenPipelineService | intent, message, plan, app_info, logo_generated, app_generated, done |
| 应用编辑 | POST /api/agent/unified | EDIT | AppEditPipelineService | intent, message, done |
| 用户认证 | POST /api/auth/login | - | - | - |
| 应用预览 | GET /api/preview/{uuid} | - | - | - |

### SSE 事件类型详解

| 事件名 | 触发时机 | 数据格式 |
|--------|----------|----------|
| `intent` | 意图识别完成 | `{"type": "CHAT\|GEN\|EDIT"}` |
| `message` | 文本流片段 | `{"content": "..."}` |
| `plan` | 方案生成片段 | `{"content": "..."}` |
| `app_info` | 应用信息提取完成 | `{"name": "...", "description": "..."}` |
| `logo_generated` | Logo 生成完成 | `{"logoUuid": "...", "ext": "png"}` |
| `app_generated` | 应用生成完成 | `{"uuid": "...", "url": "/api/preview/..."}` |
| `done` | 流结束 | `{}` |
| `error` | 错误发生 | `{"message": "..."}` |
