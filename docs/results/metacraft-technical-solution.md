# MetaCraft 项目完整技术方案

## 项目概述

MetaCraft 是一个全栈 AI 驱动应用生成平台，用户可以通过自然语言对话创建 Web 应用程序。系统使用 LangChain4j 进行 AI 集成，通过意图识别自动判断用户是想聊天还是生成应用。核心哲学是 AI 助手（元梦/YuanMeng）可以自动保存生成的应用，用户只需描述需求，AI 即可完成代码生成和持久化的整个流程。

**技术栈**:

- 后端: Java 21, Spring Boot 3.5.9, LangChain4j 1.11.0-beta19
- 前端 (鸿蒙): ArkTS (TypeScript 变体), ETS, RCP
- 数据库: PostgreSQL + Flyway
- AI: DashScope/Qwen 模型

---

# 第一部分：后端 Spring Boot API 技术方案

## 一、项目架构概览

### 1.1 模块结构

```
apps/api/src/main/java/com/metacraft/api/
├── MetaCraftApiApplication.java          # Spring Boot 启动类
├── config/                                # 配置类
│   ├── SecurityConfig.java                # Spring Security 配置
│   ├── OpenApiConfig.java                 # Swagger 配置
│   └── WebMvcConfig.java                  # Web MVC 配置
├── exception/                             # 异常处理
│   ├── GlobalExceptionHandler.java        # 全局异常处理器
│   └── UnauthorizedException.java         # 未授权异常
├── response/                              # 统一响应
│   ├── ApiResponse.java                   # API 响应封装
│   └── Response.java                      # 响应构建器
├── security/                              # 安全模块
│   ├── JwtTokenProvider.java              # JWT 令牌提供器
│   ├── JwtAuthenticationFilter.java       # JWT 认证过滤器
│   ├── AuthUtils.java                     # 认证工具类
│   └── CustomUserDetails.java              # 用户详情
└── modules/                               # 业务模块
    ├── ai/                                # AI 智能体模块
    ├── app/                               # 应用管理模块
    ├── user/                              # 用户认证模块
    └── storage/                           # 存储服务模块
```

### 1.2 数据库架构

系统使用 Flyway 进行数据库迁移，共有 8 个迁移文件：

| 迁移文件 | 说明 |
|----------|------|
| V1__init_base.sql | 初始化基础表 |
| V2__create_user_tables.sql | 用户表 |
| V3__create_apps_and_versions_table.sql | 应用和版本表 |
| V4__create_chat_sessions_table.sql | 聊天会话表 |
| V5__create_chat_messages_table.sql | 聊天消息表 |
| V6__add_logo_to_apps.sql | 添加 logo 字段 |
| V7__add_opencode_session_id_to_apps.sql | 添加 OpenCode 会话 ID |
| V8__add_store_tables.sql | 商店相关表（评分、评论） |

**核心表结构**:

```sql
-- 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    avatar_base64 TEXT,
    bio TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 应用表
CREATE TABLE apps (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    description TEXT,
    logo VARCHAR(128),
    open_code_session_id VARCHAR(128),
    is_public BOOLEAN DEFAULT FALSE,
    current_version_id BIGINT,
    average_rating DOUBLE PRECISION,
    rating_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_apps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 应用版本表
CREATE TABLE app_versions (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    change_log TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_app_versions_app FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE CASCADE
);

-- 聊天会话表
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    app_id BIGINT,
    title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sessions_app FOREIGN KEY (app_id) REFERENCES apps(id) ON DELETE SET NULL
);

-- 聊天消息表
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,  -- 'user' 或 'assistant'
    type VARCHAR(20),           -- 'text' 或 'app'
    content TEXT NOT NULL,
    related_app_id BIGINT,
    related_version_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

-- 应用评分表
CREATE TABLE app_ratings (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (app_id, user_id)
);

-- 应用评论表
CREATE TABLE app_comments (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 二、AI 智能体模块 (AI Module)

### 2.1 模块职责

AI 智能体模块是 MetaCraft 的核心，负责处理用户的自然语言输入，识别意图，并执行相应的操作。该模块采用管道架构，将不同的处理逻辑解耦，支持聊天、应用生成和应用编辑三种场景。

**核心功能**:

1. 意图识别 - 分析用户输入，判断是聊天、生成应用还是编辑应用
2. 聊天对话 - 处理日常对话场景
3. 应用生成 - 根据用户描述生成完整的 Web 应用
4. 应用编辑 - 修改已有应用的代码
5. SSE 流式响应 - 实时推送处理进度和结果

### 2.2 核心组件详解

#### 2.2.1 统一入口控制器 (UnifiedController)

**文件位置**: `modules/ai/controller/UnifiedController.java`

**职责**: 作为 AI 智能体的统一入口，接收用户请求并返回 SSE 流式响应。

**API 端点**: `POST /api/ai/agent/unified`

```java
@RestController
@RequestMapping("/ai/agent")
@Tag(name = "AI 智能体", description = "AI 智能体相关接口(仅流式输出)")
public class UnifiedController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UnifiedOrchestrator unifiedOrchestrator;

    @PostMapping(value = "/unified")
    @Operation(summary = "智能体统一入口", description = "SSE 流式统一入口，自动识别意图并流式返回")
    public Flux<ServerSentEvent<String>> unified(
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        return unifiedOrchestrator.handleRequest(request, userId);
    }
}
```

**请求 DTO**:

```java
public class AgentRequestDTO {
    @NotBlank(message = "Message cannot be blank")
    private String message;
    
    private String sessionId;  // 可选，用于继续对话
}
```

**响应类型** (SSE Event Types):

| Event | Description | Payload Structure |
|-------|-------------|-------------------|
| `intent` | 意图分类结果 | `{"intent": "CHAT\|GEN\|EDIT"}` |
| `message` | 流式 AI 响应 | `{"content": "响应内容"}` |
| `plan` | 应用生成计划 | `{"plan": "计划内容"}` |
| `app_info` | 应用元信息 | `{"name": "应用名", "description": "描述"}` |
| `logo_generated` | Logo 生成完成 | `{"uuid": "logo UUID", "ext": "png"}` |
| `app_generated` | 应用生成完成 | `{"uuid": "app UUID", "version": 1}` |
| `done` | 流结束 | `{}` |
| `error` | 错误信息 | `{"error": "错误消息"}` |

#### 2.2.2 统一编排器 (UnifiedOrchestrator)

**文件位置**: `modules/ai/service/UnifiedOrchestrator.java`

**职责**: 编排整个请求处理流程，包括意图分析、管道选择和错误处理。

**核心流程**:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedOrchestrator {
    private static final int HISTORY_WINDOW_SIZE = 20;

    private final IntentAnalyzer intentAnalyzer;
    private final SessionTitleGenerator sessionTitleGenerator;
    private final SseUtils sseUtils;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatPipelineService chatPipelineService;
    private final AppGenPipelineService appGenPipelineService;
    private final AppEditPipelineService appEditPipelineService;

    public Flux<ServerSentEvent<String>> handleRequest(AgentRequestDTO request, Long userId) {
        return Mono.fromCallable(() -> prepareAndSaveUserMessage(request, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(context -> 
                    Mono.fromCallable(() -> intentAnalyzer.analyze(context.message()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMapMany(intent -> {
                            // 根据意图选择不同的处理管道
                            return switch (intent) {
                                case CHAT -> chatPipelineService.execute(...);
                                case GEN -> appGenPipelineService.execute(...);
                                case EDIT -> appEditPipelineService.execute(...);
                            };
                        }))
                .onErrorResume(e -> Flux.just(errorEvent))
                .concatWithValues(doneEvent);
    }
}
```

**意图识别结果**:

```java
public record IntentDTO(String name) {
    // name 可以是: CHAT, GEN, EDIT
}
```

**关键处理逻辑**:

1. **消息保存**: 在分析意图前，先保存用户消息到数据库
2. **会话管理**: 如果没有提供 sessionId，自动创建新会话
3. **标题生成**: 如果会话标题是默认的，使用 AI 生成有意义的标题
4. **历史上下文**: 保留最近 20 条对话作为上下文

#### 2.2.3 意图分析器 (IntentAnalyzer)

**文件位置**: `modules/ai/agent/IntentAnalyzer.java`

**职责**: 使用 LangChain4j 的 @AiService 模式进行意图分类。

**实现方式**:

```java
@AiService
public interface IntentAnalyzer {
    @SystemMessage(fromResource = "prompts/intent.txt")
    @UserMessage("User request: {{it}}")
    IntentDTO analyze(String message);
}
```

**提示词模板** (`prompts/intent.txt`):

```
你是一个意图分析助手。根据用户的需求，分类为以下意图之一：
- CHAT: 用户想要进行普通聊天或问答
- GEN: 用户想要创建一个新的 Web 应用
- EDIT: 用户想要修改已有应用的代码

只返回一个词：CHAT、GEN 或 EDIT
```

#### 2.2.4 聊天管道 (ChatPipelineService)

**文件位置**: `modules/ai/service/pipeline/ChatPipelineService.java`

**职责**: 处理纯聊天场景，将用户消息发送给 AI 并获取流式响应。

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPipelineService {

    private final ChatAgent chatAgent;
    private final SseUtils sseUtils;
    private final ChatMessageService chatMessageService;

    public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
        StringBuilder assistantReply = new StringBuilder();

        return chatAgent.chat(message, history)
                .doOnNext(assistantReply::append)
                .doOnComplete(() -> saveAssistantMessage(userId, sessionId, assistantReply.toString()))
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(sseUtils.toContentJson(chunk))
                        .build());
    }

    private void saveAssistantMessage(Long userId, String sessionId, String content) {
        if (content == null || content.isBlank()) {
            log.warn("Skip saving empty assistant message for session {}", sessionId);
            return;
        }
        // 保存助手消息到数据库
    }
}
```

#### 2.2.5 应用生成管道 (AppGenPipelineService)

**文件位置**: `modules/ai/service/pipeline/AppGenPipelineService.java`

**职责**: 处理应用生成场景，这是 MetaCraft 的核心业务逻辑。

**处理流程图**:

```
用户请求 (生成应用)
        │
        ▼
┌───────────────────┐
│  1. 模板匹配      │ ──► templateMatcherService.matchTemplate()
│  (并行执行)       │
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  2. AI 对话       │ ──► chatAgent.chatBeforeGen()
│  (并行执行)       │
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  3. 生成计划      │ ──► planGenerator.generatePlan()
│  (并行执行)       │
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  4. 提取应用信息  │ ──► appInfoExtractor.extract()
│                   │     appService.createApp()
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  5. 生成 Logo     │ ──► imageService.generateLogoAndSave()
│  (可选)           │
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  6. 创建版本      │ ──► 有模板: createVersionFromTemplate()
│                   │ ──► 无模板: generateInitialVersionWithOpenCode()
└───────────────────┘
        │
        ▼
   SSE: app_generated
```

**核心代码**:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AppGenPipelineService {

    @Value("${app.templates.matched-response-delay-ms:3000}")
    private long templateMatchedResponseDelayMs;

    private final ChatAgent chatAgent;
    private final PlanGenerator planGenerator;
    private final AppInfoExtractor appInfoExtractor;
    private final AppService appService;
    private final ImageService imageService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final OpenCodeClient openCodeClient;
    private final OpenCodePromptService openCodePromptService;
    private final OpenCodeTemplateService openCodeTemplateService;
    private final OpenCodeWorkspaceService openCodeWorkspaceService;
    private final SseUtils sseUtils;
    private final TemplateMatcherService templateMatcherService;
    private final TemplateFileService templateFileService;

    public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
        // 使用 AtomicReference 捕获各阶段结果
        AtomicReference<Long> relatedAppIdRef = new AtomicReference<>();
        AtomicReference<Long> relatedVersionIdRef = new AtomicReference<>();
        AtomicReference<String> matchedTemplateRef = new AtomicReference<>();

        // 1. 模板匹配 (并行)
        Mono<String> templateMatchMono = templateMatcherService.matchTemplate(message)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(matchedTemplateRef::set)
                .cache();

        // 2. AI 对话流
        Flux<ServerSentEvent<String>> chatStream = chatAgent.chatBeforeGen(message, history)
                .map(chunk -> event("message", chunk));

        // 3. 计划生成流
        Flux<ServerSentEvent<String>> planStream = planGenerator.generatePlan(message)
                .map(chunk -> event("plan", chunk));

        // 4. 应用信息流
        Mono<AppEntity> appInfoMono = Mono.fromCallable(() -> createAppFromExtractedInfo(userId, sessionId, message))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(app -> relatedAppIdRef.set(app.getId()))
                .cache();

        Flux<ServerSentEvent<String>> appInfoStream = appInfoMono.map(app -> 
            event("app_info", sseUtils.toAppInfoJson(app.getName(), app.getDescription()))).flux();

        // 5. 生成 Logo 和代码 (等待模板匹配完成后)
        Flux<ServerSentEvent<String>> postAppInfoStream = templateMatchMono
                .thenMany(appInfoMono.flatMapMany(app -> {
                    // 生成 Logo
                    Flux<ServerSentEvent<String>> logoStream = generateLogoEvent(app);
                    
                    // 创建版本 (模板或 OpenCode)
                    Mono<ServerSentEvent<String>> codeStream = createVersionMono(...);
                    
                    return Flux.merge(logoStream, codeStream);
                }));

        // 合并所有流
        return Flux.merge(chatStream, planStream, appInfoStream, postAppInfoStream)
                .doOnComplete(() -> saveAssistantGenMessage(...));
    }
}
```

**模板匹配响应延迟**:

当模板匹配成功时，系统会延迟响应（默认 3000ms），让用户感知到模板匹配的结果：

```java
private Duration resolveTemplateResponseDelay(String matchedTemplate) {
    if (matchedTemplate == null || matchedTemplate.isBlank()) {
        return Duration.ZERO;
    }
    return Duration.ofMillis(templateMatchedResponseDelayMs);
}
```

#### 2.2.6 应用编辑管道 (AppEditPipelineService)

**文件位置**: `modules/ai/service/pipeline/AppEditPipelineService.java`

**职责**: 处理应用编辑场景，允许用户修改已有应用的代码。

#### 2.2.7 聊天代理 (ChatAgent)

**文件位置**: `modules/ai/agent/ChatAgent.java`

**职责**: 使用 LangChain4j 与 AI 模型进行对话。

```java
@AiService
public interface ChatAgent {
    @SystemMessage(fromResource = "prompts/agent-chat.txt")
    Flux<String> chat(@UserMessage String message, @V("history") String history);

    @SystemMessage(fromResource = "prompts/agent-gen-app.txt")
    Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
}
```

#### 2.2.8 计划生成器 (PlanGenerator)

**文件位置**: `modules/ai/agent/PlanGenerator.java`

**职责**: 生成应用开发计划，以结构化方式呈现应用的功能和实现思路。

```java
@AiService
public interface PlanGenerator {
    @SystemMessage("你是一个应用规划专家。根据用户的需求，生成一个清晰的应用开发计划。")
    @UserMessage("用户需求: {{it}}")
    Flux<String> generatePlan(String message);
}
```

#### 2.2.9 应用信息提取器 (AppInfoExtractor)

**文件位置**: `modules/ai/agent/AppInfoExtractor.java`

**职责**: 从用户输入中提取应用名称和描述。

```java
@AiService
public interface AppInfoExtractor {
    @SystemMessage("从用户需求中提取应用名称和简要描述。")
    @UserMessage("用户需求: {{it}}")
    AppInfoDTO extract(String message);
}
```

```java
public record AppInfoDTO(String name, String description) {}
```

#### 2.2.10 会话标题生成器 (SessionTitleGenerator)

**文件位置**: `modules/ai/agent/SessionTitleGenerator.java`

**职责**: 使用 AI 生成有意义的会话标题。

```java
@AiService
public interface SessionTitleGenerator {
    @SystemMessage("根据用户的第一条消息生成一个简洁的会话标题（不超过20个字）。")
    @UserMessage("用户消息: {{it}}")
    String generateTitle(String message);
}
```

### 2.3 聊天会话服务 (ChatSessionService)

**文件位置**: `modules/ai/service/ChatSessionService.java`

**核心方法**:

```java
public class ChatSessionService {
    // 创建会话
    public ChatSessionVO createSession(Long userId, ChatSessionCreateDTO dto);
    
    // 获取会话
    public ChatSessionVO getSession(Long userId, String sessionId);
    
    // 更新会话
    public void updateSession(Long userId, String sessionId, ChatSessionUpdateDTO dto);
    
    // 删除会话
    public void deleteSession(Long userId, String sessionId);
    
    // 获取用户所有会话
    public List<ChatSessionVO> getUserSessions(Long userId);
    
    // 绑定应用到会话
    public void bindRelatedApp(Long userId, String sessionId, Long appId);
    
    // 构建历史对话
    public String buildRecentConversationHistory(Long userId, String sessionId, int limit);
}
```

**会话实体**:

```java
@Entity
@Table(name = "chat_sessions")
public class ChatSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 64)
    private String uuid;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "app_id")
    private Long appId;
    
    @Column
    private String title;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

### 2.4 聊天消息服务 (ChatMessageService)

**文件位置**: `modules/ai/service/ChatMessageService.java`

**核心方法**:

```java
public class ChatMessageService {
    // 保存消息
    public void saveMessage(Long userId, ChatMessageCreateDTO dto);
    
    // 获取会话消息
    public List<ChatMessageVO> getSessionMessages(Long userId, String sessionId);
    
    // 构建历史对话
    public String buildRecentConversationHistory(Long userId, String sessionId, int limit);
}
```

**消息实体**:

```java
@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    
    @Column(nullable = false, length = 20)
    private String role;  // 'user' 或 'assistant'
    
    @Column(length = 20)
    private String type;  // 'text' 或 'app'
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "related_app_id")
    private Long relatedAppId;
    
    @Column(name = "related_version_id")
    private Long relatedVersionId;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
```

---

## 三、应用管理模块 (App Management Module)

### 3.1 模块职责

应用管理模块负责管理用户创建的应用，包括应用的 CRUD 操作、版本管理、预览服务和商店功能。

**核心功能**:

1. 应用生命周期管理 - 创建、更新、删除应用
2. 版本控制 - 管理应用的多个版本
3. 预览服务 - 提供应用实时预览
4. 商店功能 - 应用的发布、评分、评论
5. 资源管理 - Logo 和静态资源

### 3.2 应用控制器 (AppController)

**文件位置**: `modules/app/controller/AppController.java`

**API 端点详情**:

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/apps | 创建新应用 | 需要 |
| GET | /api/apps | 获取用户所有应用 | 需要 |
| GET | /api/apps/{appId} | 获取应用详情 (ID) | 需要 |
| GET | /api/apps/uuid/{uuid} | 获取应用详情 (UUID) | 需要 |
| PATCH | /api/apps/{appId} | 更新应用 | 需要 |
| DELETE | /api/apps/{appId} | 删除应用 | 需要 |
| GET | /api/apps/{appId}/versions | 获取所有版本 | 需要 |
| GET | /api/apps/{appId}/versions/{versionNumber} | 获取指定版本 | 需要 |
| DELETE | /api/apps/{appId}/versions/{versionId} | 删除版本 | 需要 |

**创建应用示例**:

```json
POST /api/apps
{
  "name": "计算器应用",
  "description": "一个简单实用的计算器"
}
```

**响应**:

```json
{
  "success": true,
  "message": "App created successfully",
  "data": {
    "id": 1,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "计算器应用",
    "description": "一个简单实用的计算器",
    "logo": null,
    "isPublic": false,
    "currentVersionId": 1,
    "versions": [
      {
        "id": 1,
        "versionNumber": 1,
        "changeLog": "初始化版本",
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### 3.3 应用实体 (AppEntity)

**文件位置**: `modules/app/entity/AppEntity.java`

**完整字段定义**:

```java
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "apps")
public class AppEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String uuid;  // 对外展示的唯一标识

    @Column(name = "user_id", nullable = false)
    private Long userId;  // 所有者

    @Column(columnDefinition = "TEXT")
    private String name;  // 应用名称

    @Column(columnDefinition = "TEXT")
    private String description;  // 应用描述

    @Column(name = "logo", length = 128)
    private String logo;  // Logo 文件名

    @Column(name = "open_code_session_id", length = 128)
    private String openCodeSessionId;  // OpenCode 会话 ID

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;  // 是否公开

    @Column(name = "current_version_id")
    private Long currentVersionId;  // 当前版本 ID

    @Column(name = "average_rating")
    private Double averageRating;  // 平均评分 (缓存)

    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;  // 评分数量

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

### 3.4 应用版本实体 (AppVersionEntity)

**文件位置**: `modules/app/entity/AppVersionEntity.java`

```java
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_versions")
public class AppVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;  // 版本号

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;  // 代码存储路径

    @Column(name = "change_log", columnDefinition = "TEXT")
    private String changeLog;  // 变更日志

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

### 3.5 应用服务 (AppService)

**文件位置**: `modules/app/service/AppService.java`

**完整方法列表**:

```java
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppApplicationService appApplicationService;
    private final AppVersionService appVersionService;
    private final AppQueryService appQueryService;

    // ========== 创建操作 ==========
    
    /**
     * 创建新应用
     */
    public AppEntity createApp(Long userId, String name, String description);
    
    /**
     * 创建新版本
     */
    public AppVersionEntity createVersion(Long appId, String htmlContent, String jsContent, String changeLog);
    
    /**
     * 从快照创建版本
     */
    public AppVersionEntity createVersionFromSnapshot(Long appId, String htmlContent, String jsContent, String changeLog);
    
    /**
     * 创建应用 (使用 DTO)
     */
    public AppVO createApp(Long userId, AppCreateDTO dto);

    // ========== 查询操作 ==========
    
    /**
     * 获取应用实体
     */
    public AppEntity getApp(Long appId);
    
    /**
     * 获取用户最新创建的应用
     */
    public AppEntity getLatestAppByUserId(Long userId);
    
    /**
     * 根据 Logo UUID 获取应用
     */
    public AppEntity getLatestAppByUserIdAndLogoUuid(Long userId, String logoUuid);
    
    /**
     * 获取用户所有应用
     */
    public List<AppVO> getUserApps(Long userId);
    
    /**
     * 根据 UUID 获取应用
     */
    public AppVO getAppByUuid(Long userId, String uuid);
    
    /**
     * 根据 ID 获取应用
     */
    public AppVO getAppById(Long userId, Long appId);
    
    /**
     * 获取当前代码
     */
    public AppCodeSnapshotDTO getCurrentCode(Long userId, Long appId);
    
    /**
     * 获取应用所有版本
     */
    public List<AppVersionVO> getAppVersions(Long userId, Long appId);
    
    /**
     * 获取指定版本
     */
    public AppVersionVO getVersion(Long userId, Long appId, Integer versionNumber);

    // ========== 更新操作 ==========
    
    /**
     * 更新应用元数据
     */
    public void updateAppMetadata(Long userId, Long appId, String name, String description);
    
    /**
     * 更新应用 Logo
     */
    public void updateAppLogo(Long appId, String logo);
    
    /**
     * 绑定 OpenCode 会话 ID
     */
    public void bindOpenCodeSessionId(Long appId, String openCodeSessionId);
    
    /**
     * 更新应用
     */
    public AppVO updateApp(Long userId, Long appId, AppUpdateDTO dto);

    // ========== 删除操作 ==========
    
    /**
     * 删除应用 (包括所有版本和文件)
     */
    public void deleteApp(Long userId, Long appId);
    
    /**
     * 删除指定版本
     */
    public void deleteVersion(Long userId, Long appId, Long versionId);
}
```

### 3.6 预览控制器 (PreviewController)

**文件位置**: `modules/app/controller/PreviewController.java`

**职责**: 提供应用实时预览功能，支持最新版本和指定版本。

**API 端点**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/preview/{uuid} | 预览最新版本 |
| GET | /api/preview/{uuid}/v/{versionNumber} | 预览指定版本 |
| GET | /api/preview/{uuid}/{fileName}.{extension} | 预览静态资源 (最新) |
| GET | /api/preview/{uuid}/v/{versionNumber}/{fileName}.{extension} | 预览静态资源 (指定版本) |

**核心代码**:

```java
@RestController
@RequestMapping("/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;

    /**
     * 预览应用最新版本
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<String> previewLatest(@PathVariable String uuid, HttpServletRequest request) {
        PreviewResource resource = previewService.getLatestHtml(uuid);
        String htmlWithBase = injectBaseHref(resource.content(), buildBaseHref(request));

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(resource.mediaType()))
            .header("X-Frame-Options", "ALLOW-FROM *")
            .header("Content-Security-Policy", "frame-ancestors *")
            .body(htmlWithBase);
    }

    /**
     * 预览指定版本
     */
    @GetMapping("/{uuid}/v/{versionNumber}")
    public ResponseEntity<String> previewVersion(
            @PathVariable String uuid,
            @PathVariable Integer versionNumber,
            HttpServletRequest request) {
        PreviewResource resource = previewService.getVersionHtml(uuid, versionNumber);
        String htmlWithBase = injectBaseHref(resource.content(), buildBaseHref(request));
        
        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(resource.mediaType()))
            .header("X-Frame-Options", "ALLOW-FROM *")
            .header("Content-Security-Policy", "frame-ancestors *")
            .body(htmlWithBase);
    }

    /**
     * 注入 base 标签
     */
    private String injectBaseHref(String htmlContent, String baseHref) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return htmlContent;
        }
        String baseTag = "<base href='" + baseHref + "'>";
        if (htmlContent.contains("<head>")) {
            return htmlContent.replaceFirst("<head>", "<head>" + baseTag);
        }
        if (htmlContent.contains("<head ")) {
            return htmlContent.replaceFirst("<head[^>]*>", "$0" + baseTag);
        }
        return baseTag + htmlContent;
    }
}
```

**关键特性**:

1. **自动注入 base 标签**: 确保相对路径的资源能正确加载
2. **Iframe 支持**: 设置 `X-Frame-Options: ALLOW-FROM *` 允许嵌入
3. **CSP 配置**: 设置 `frame-ancestors *` 允许跨域嵌入

### 3.7 商店控制器 (StoreController)

**文件位置**: `modules/app/controller/StoreController.java`

**职责**: 管理应用的发布、评分和评论功能。

**API 端点**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/store/apps | 获取已发布应用列表 |
| GET | /api/store/apps/{id} | 获取应用详情 |
| POST | /api/store/apps/{id}/publish | 发布应用 |
| DELETE | /api/store/apps/{id}/publish | 取消发布 |
| POST | /api/store/apps/{id}/ratings | 评分应用 |
| POST | /api/store/apps/{id}/comments | 添加评论 |
| DELETE | /api/store/apps/{id}/comments/{commentId} | 删除评论 |

**评分请求**:

```json
POST /api/store/apps/1/ratings
{
  "rating": 5
}
```

**评分约束**: 1-5 分，每个用户只能评分一次

**评论请求**:

```json
POST /api/store/apps/1/comments
{
  "content": "这是一个很棒的应用！"
}
```

### 3.8 Logo 控制器 (LogoController)

**文件位置**: `modules/app/controller/LogoController.java`

**职责**: 提供 Logo 图片访问。

**API 端点**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/logo/{logoUuid} | 获取 Logo 图片 |

---

## 四、用户认证模块 (User Authentication Module)

### 4.1 模块职责

用户认证模块负责处理用户注册、登录和信息管理。

**核心功能**:

1. 用户注册 - 创建新用户账号
2. 用户登录 - 验证凭据并发放 JWT
3. 用户信息管理 - 查看和更新个人信息
4. 密码管理 - 修改密码

### 4.2 认证控制器 (AuthController)

**文件位置**: `modules/user/controller/AuthController.java`

**API 端点**:

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |

**注册请求**:

```json
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "name": "用户名",
  "avatarBase64": "可选的头像 Base64",
  "bio": "可选的个人简介"
}
```

**登录请求**:

```json
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

**登录响应**:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 8640000
  }
}
```

### 4.3 用户服务 (UserService)

**文件位置**: `modules/user/service/UserService.java`

**完整方法**:

```java
@Service
public class UserService {

    /**
     * 用户注册
     */
    @Transactional
    public AuthTokenVO register(UserRegisterDTO dto) {
        // 1. 检查邮箱是否已存在
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("用户已存在");
        }
        
        // 2. 创建用户实体
        UserEntity user = new UserEntity();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setAvatarBase64(dto.getAvatarBase64() != null ? dto.getAvatarBase64() : "");
        user.setBio(dto.getBio() != null ? dto.getBio() : "");
        
        // 3. 保存用户
        userRepository.save(user);
        
        // 4. 生成 JWT
        String token = jwtTokenProvider.generateToken(user.getId());
        return new AuthTokenVO(token, jwtExpiration / 1000);
    }

    /**
     * 用户登录
     */
    public AuthTokenVO login(UserLoginDTO dto) {
        // 1. 查找用户
        UserEntity user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 2. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }
        
        // 3. 生成 JWT
        String token = jwtTokenProvider.generateToken(user.getId());
        return new AuthTokenVO(token, jwtExpiration / 1000);
    }

    /**
     * 根据 ID 获取用户
     */
    public UserVO getUserById(Long id) { ... }

    /**
     * 获取当前用户
     */
    public UserVO getCurrentUser(String email) { ... }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserVO updateUser(String email, UserUpdateDTO dto) {
        // 支持更新: 邮箱、昵称、头像、简介、密码
    }
}
```

### 4.4 JWT 令牌提供者 (JwtTokenProvider)

**文件位置**: `apps/api/src/main/java/com/metacraft/api/security/JwtTokenProvider.java`

**实现**:

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成 JWT 令牌
     */
    public String generateToken(Long userId) {
        return JWT.create()
                .withSubject(String.valueOf(userId))  // 用户 ID 作为主题
                .withIssuedAt(new Date())              // 签发时间
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))  // 过期时间
                .sign(Algorithm.HMAC256(secret));     // HMAC256 签名
    }

    /**
     * 从令牌获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);
        return Long.parseLong(jwt.getSubject());
    }

    /**
     * 验证令牌有效性
     */
    public boolean validateToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(token);
            String subject = jwt.getSubject();
            return subject != null && subject.matches("^\\d+$");
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
```

**JWT 配置** (`application.yaml`):

```yaml
jwt:
  secret: ${JWT_SECRET:change-this-secret-key-in-production}
  expiration: ${JWT_EXPIRATION:8640000000}  # 毫秒，默认约 100 天
```

### 4.5 安全配置 (SecurityConfig)

**文件位置**: `apps/api/src/main/java/com/metacraft/api/config/SecurityConfig.java`

**配置**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configure(http))  // 启用 CORS
                .csrf(AbstractHttpConfigurer::disable)  // 禁用 CSRF (支持 SSE)
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 无状态
                .authorizeHttpRequests(auth -> auth
                    // 公开端点
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/ws/yuanmeng/**").permitAll()
                    .requestMatchers("/preview/**").permitAll()
                    .requestMatchers("/api/preview/**").permitAll()
                    .requestMatchers("/logo/**").permitAll()
                    .requestMatchers("/api/logo/**").permitAll()
                    .requestMatchers("/public/**").permitAll()
                    .requestMatchers("/api/health").permitAll()
                    // AI 端点手动验证
                    .requestMatchers("/api/ai/agent/**").permitAll()
                    // Swagger
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", 
                                    "/swagger-ui.html", "/api/v3/api-docs/**").permitAll()
                    // 其他需要认证
                    .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**关键配置说明**:

1. **CORS 启用**: 支持跨域请求
2. **CSRF 禁用**: SSE 流式响应需要
3. **无状态会话**: JWT 认证不需要会话
4. **公开端点**: 认证、预览、Logo、Swagger
5. **AI 端点**: 手动验证避免 Security 异常

### 4.6 用户实体 (UserEntity)

**文件位置**: `modules/user/entity/UserEntity.java`

```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column
    private String name;

    @Column(name = "avatar_base64", columnDefinition = "TEXT")
    private String avatarBase64;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

---

## 五、存储服务模块 (Storage Service Module)

### 5.1 存储服务 (StorageService)

**文件位置**: `modules/storage/service/StorageService.java`

**职责**: 提供安全的文件系统操作服务。

**核心方法**:

```java
@Service
public class StorageService {

    @Value("${app.storage.path:data}")
    private String storagePath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(this.rootLocation);
    }

    /**
     * 保存文本文件
     */
    public String saveTextFile(String relativePath, String content) {
        Path destinationFile = this.rootLocation.resolve(relativePath).normalize();
        
        // 安全检查：防止目录遍历攻击
        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside current directory.");
        }

        Files.createDirectories(destinationFile.getParent());
        Files.write(destinationFile, content.getBytes(StandardCharsets.UTF_8), 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        return destinationFile.toString();
    }

    /**
     * 读取文本文件
     */
    public String readTextFile(String relativePath) {
        Path file = this.rootLocation.resolve(relativePath).normalize();
        if (!file.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot read file outside current directory.");
        }
        if (Files.notExists(file)) {
            throw new RuntimeException("File not found: " + relativePath);
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /**
     * 删除文件
     */
    public void deleteFile(String relativePath) {
        Path file = this.rootLocation.resolve(relativePath).normalize();
        if (!file.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot delete file outside current directory.");
        }
        if (Files.exists(file)) {
            Files.delete(file);
        }
    }

    /**
     * 保存二进制文件
     */
    public String saveBinaryFile(String relativePath, byte[] content) { ... }

    /**
     * 读取二进制文件
     */
    public byte[] readBinaryFile(String relativePath) { ... }

    /**
     * 检查文件是否存在
     */
    public boolean exists(String relativePath) { ... }
}
```

**安全特性 - 目录遍历防护**:

```java
// 关键代码：确保文件路径在存储目录下
Path destinationFile = this.rootLocation.resolve(relativePath).normalize();
if (!destinationFile.startsWith(this.rootLocation)) {
    throw new SecurityException("Cannot store file outside current directory.");
}
```

**存储目录结构**:

```
storage/                          # 根目录 (可配置)
├── apps/                         # 应用代码
│   ├── {appId}/                  # 按应用 ID 隔离
│   │   ├── v1/                   # 版本 1
│   │   │   ├── index.html        # 主页面
│   │   │   ├── app.js            # 应用脚本
│   │   │   └── assets/           # 静态资源
│   │   │       ├── styles.css
│   │   │       └── logo.png
│   │   └── v2/                   # 版本 2
│   │       └── ...
│   └── {appId}/
│       └── ...
└── logos/                        # Logo 文件
    └── {uuid}.png
```

---

# 第二部分：鸿蒙端 (HarmonyOS App) 技术方案

## 一、项目架构概述

**模块位置**: `apps/huawei/entry/src/main/ets/`

**技术栈**:

- ArkTS (TypeScript 变体)
- ETS (Extensible TypeScript) - UI 框架
- OHPM 包管理
- RCP (Remote Communication Kit) - 网络请求

**目录结构**:

```
entry/src/main/ets/
├── core/                              # 核心模块
│   └── http/
│       ├── ApiConfig.ets              # API 配置
│       ├── HttpManager.ets           # HTTP 管理器
│       ├── Interceptors.ets           # 拦截器
│       └── SSEClient.ets              # SSE 客户端
├── features/                          # 功能模块
│   ├── auth/                          # 认证模块
│   │   ├── model/
│   │   │   └── AuthModel.ets
│   │   └── service/
│   │       └── AuthService.ets
│   ├── chat/                          # 聊天模块
│   │   ├── adapter/
│   │   │   ├── ChatMessageMapper.ets
│   │   │   └── GenMessageParser.ets
│   │   ├── components/
│   │   │   ├── ChatPanel.ets          # 聊天面板
│   │   │   ├── MessageItem.ets        # 消息项
│   │   │   ├── MessageMarkdownCard.ets
│   │   │   ├── GenMessageCard.ets
│   │   │   └── AppPlanCard.ets
│   │   ├── domain/
│   │   │   └── ChatMessage.ets
│   │   ├── request/
│   │   │   └── AiChatRequestMapper.ets
│   │   ├── service/
│   │   │   └── ChatStreamService.ets
│   │   ├── state/
│   │   │   ├── ChatMessageState.ets
│   │   │   └── ChatStreamDraft.ets
│   │   └── types.ets
│   ├── session/                       # 会话管理模块
│   │   ├── components/
│   │   │   ├── SessionList.ets
│   │   │   └── SessionSidebar.ets
│   │   ├── domain/
│   │   │   └── Session.ets
│   │   ├── request/
│   │   │   └── SessionRequests.ets
│   │   ├── service/
│   │   │   └── SessionService.ets
│   │   └── state/
│   │       └── SessionGroups.ets
│   ├── app-center/                    # 应用中心模块
│   │   ├── components/
│   │   ├── domain/
│   │   ├── pages/
│   │   │   ├── AppStoreScreen.ets     # 应用商店
│   │   │   ├── ExploreScreen.ets      # 发现页
│   │   │   ├── MyAppsScreen.ets       # 我的应用
│   │   │   └── PreviewScreen.ets      # 预览页
│   │   ├── request/
│   │   ├── service/
│   │   └── state/
│   ├── user-center/                   # 用户中心模块
│   │   ├── pages/
│   │   │   ├── ProfileScreen.ets
│   │   │   └── SettingsScreen.ets
│   │   └── state/
│   ├── workspace/                      # 工作区模块
│   │   ├── components/
│   │   │   └── YuanDreamWorkspacePlaceholder.ets
│   │   ├── pages/
│   │   │   └── WorkspaceScreen.ets
│   │   └── state/
│   └── data-center/                   # 数据中心模块
│       └── pages/
│           └── DataCenterScreen.ets
├── pages/                             # 页面入口
│   ├── Index.ets
│   ├── AppStore.ets
│   ├── DataCenter.ets
│   ├── Explore.ets
│   ├── MyApps.ets
│   ├── Preview.ets
│   ├── Profile.ets
│   └── Setting.ets
├── shared/                            # 共享组件
│   ├── layout/
│   │   └── BottomSheetMenu.ets
│   ├── types/
│   │   └── Result.ets
│   └── ui/
│       ├── BasePageHeader.ets
│       └── PagePlaceholder.ets
└── entryability/
    ├── EntryAbility.ets
    └── EntryBackupAbility.ets
```

---

## 二、核心 HTTP 模块详解

### 2.1 API 配置 (ApiConfig)

**文件位置**: `core/http/ApiConfig.ets`

```typescript
export const API_BASE_URL = 'http://10.0.2.2:8080/api';

// 开发环境使用 10.0.2.2 访问主机
export function toApiUrl(path: string): string {
    if (path.startsWith('http')) {
        return path;
    }
    return `${API_BASE_URL}${path}`;
}
```

**说明**:

- `10.0.2.2` 是 Android 模拟器访问主机的特殊地址
- 鸿蒙模拟器同样适用
- 生产环境应替换为实际服务器地址

### 2.2 HTTP 管理器 (HttpManager)

**文件位置**: `core/http/HttpManager.ets`

**职责**: 统一处理 HTTP 请求，提供拦截器支持和错误处理。

### 2.3 拦截器 (Interceptors)

**文件位置**: `core/http/Interceptors.ets`

**认证拦截器**:

```typescript
import { rcp } from '@kit.RemoteCommunicationKit';
import { authService } from '../../features/auth/service/AuthService';

export class AuthInterceptor implements rcp.Interceptor {
    intercept(request: rcp.Request, next: rcp.InterceptorChain): rcp.Request {
        const token = authService.getToken();
        if (token) {
            request.headers['Authorization'] = `Bearer ${token}`;
        }
        return next.proceed(request);
    }
}
```

**未授权回调处理**:

```typescript
let unauthorizedCallback: (() => void) | null = null;

export function setOnUnauthorizedCallback(callback: () => void): void {
    unauthorizedCallback = callback;
}

export function handleUnauthorized(): void {
    if (unauthorizedCallback) {
        unauthorizedCallback();
    }
}
```

### 2.4 SSE 客户端 (SSEClient)

**文件位置**: `core/http/SSEClient.ets`

**职责**: 处理 Server-Sent Events 流式响应，是实现 AI 对话的核心组件。

**接口定义**:

```typescript
export interface SSEListener {
    onMessage: (data: string) => void;
    onError: (error: string) => void;
    onDone?: () => void;
    onIntent?: (intent: string) => void;
    onPlan?: (data: string) => void;
    onAppInfo?: (appName?: string, appDescription?: string) => void;
    onLogoGenerated?: (logoUrl: string) => void;
    onAppGenerated?: (previewUrl: string, appName?: string, 
                      appDescription?: string, logoUrl?: string) => void;
}
```

**SSE 写入流实现**:

```typescript
class SSEWriteStream implements rcp.WriteStream {
    private decoder: util.TextDecoder;
    private buffer: string = '';
    private listener: SSEListener;
    private previewUrl: string = '';
    private appName: string = '';
    private appDescription: string = '';
    private logoUrl: string = '';

    constructor(listener: SSEListener) {
        this.listener = listener;
        this.decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
    }

    async write(buffer: ArrayBuffer): Promise<number | void> {
        // 解码 chunk，使用流模式处理多字节 UTF-8 字符
        const chunk = this.decoder.decodeToString(new Uint8Array(buffer), { stream: true });
        this.buffer += chunk;
        this.buffer = this.buffer.replace(/\r\n/g, '\n');
        
        // 按事件分隔
        let eventEndIndex: number;
        while ((eventEndIndex = this.buffer.indexOf('\n\n')) !== -1) {
            const messageChunk = this.buffer.substring(0, eventEndIndex);
            this.buffer = this.buffer.substring(eventEndIndex + 2);
            this.parseSSEMessage(messageChunk);
        }
        
        return buffer.byteLength;
    }

    private parseSSEMessage(chunk: string) {
        const lines = chunk.split('\n');
        let eventName = '';
        let dataBuffer: string[] = [];

        // 解析事件行和数据行
        for (const line of lines) {
            const trimmedLine = line.trimStart();
            
            if (trimmedLine.startsWith('event:')) {
                eventName = trimmedLine.substring(6).trim();
            } else if (trimmedLine.startsWith('data:')) {
                let content = trimmedLine.substring(5);
                if (content.startsWith(' ')) {
                    content = content.substring(1);
                }
                dataBuffer.push(content);
            }
        }

        const eventData = dataBuffer.join('\n');
        
        // 解析 JSON 数据
        try {
            const parsed: Record<string, Object> = JSON.parse(eventData);
            
            // 根据事件类型调用不同的回调
            if (eventName === 'intent') {
                const intent = parsed.intent as string;
                if (this.listener.onIntent) {
                    this.listener.onIntent(intent);
                }
            } else if (eventName === 'message') {
                const content = parsed.content as string;
                if (content.length > 0) {
                    this.listener.onMessage(content);
                }
            } else if (eventName === 'plan') {
                const plan = parsed.plan as string;
                if (this.listener.onPlan && plan.length > 0) {
                    this.listener.onPlan(plan);
                }
            } else if (eventName === 'app_info') {
                this.appName = parsed.name as string;
                this.appDescription = parsed.description as string;
                if (this.listener.onAppInfo) {
                    this.listener.onAppInfo(this.appName, this.appDescription);
                }
            } else if (eventName === 'logo_generated') {
                const logoUuid = parsed.uuid as string;
                if (logoUuid && logoUuid.length > 0) {
                    this.logoUrl = toApiUrl(`/api/logo/${logoUuid}`);
                    if (this.listener.onLogoGenerated) {
                        this.listener.onLogoGenerated(this.logoUrl);
                    }
                }
            } else if (eventName === 'app_generated') {
                const appUuid = parsed.uuid as string;
                this.previewUrl = appUuid && appUuid.length > 0
                    ? toApiUrl(`/api/preview/${appUuid}`)
                    : '';
                this.notifyAppGenerated();
            } else if (eventName === 'error') {
                const error = parsed.error as string;
                if (this.listener.onError) {
                    this.listener.onError(error);
                }
            } else if (eventName === 'done') {
                if (this.listener.onDone) {
                    this.listener.onDone();
                }
            }
        } catch (e) {
            // JSON 解析失败，使用原始数据
            if (eventName === 'message' || !eventName) {
                this.listener.onMessage(eventData);
            }
        }
    }

    private notifyAppGenerated() {
        if (this.listener.onAppGenerated && this.previewUrl.length > 0) {
            this.listener.onAppGenerated(
                this.previewUrl, 
                this.appName, 
                this.appDescription, 
                this.logoUrl
            );
        }
    }
}
```

**SSE 客户端类**:

```typescript
export class SSEClient {
    private session: rcp.Session | null = null;
    private listener: SSEListener;

    constructor(listener: SSEListener) {
        this.listener = listener;
    }

    public async connect(url: string, body?: object): Promise<void> {
        // 创建 RCP 会话
        this.session = rcp.createSession({
            interceptors: [new AuthInterceptor()],
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            },
            requestConfiguration: {
                transfer: {
                    timeout: {
                        connectMs: 60000,
                        transferMs: 3600000
                    }
                }
            }
        });

        // 创建 SSE 流
        const sseStream = new SSEWriteStream({
            onMessage: this.listener.onMessage,
            onError: this.listener.onError,
            onDone: () => {
                this.close();
                if (this.listener.onDone) {
                    this.listener.onDone();
                }
            },
            onPlan: this.listener.onPlan,
            onIntent: this.listener.onIntent,
            onAppInfo: this.listener.onAppInfo,
            onLogoGenerated: this.listener.onLogoGenerated,
            onAppGenerated: this.listener.onAppGenerated
        });

        try {
            await this.session.post(url, body || {}, {
                kind: 'stream',
                stream: sseStream
            });
        } catch (err) {
            const errorObj = err as BusinessError;
            
            // 1007900992 表示请求被取消，是正常行为
            if (errorObj.code === 1007900992) {
                console.info('[SSEClient] Request canceled by client');
                return;
            }
            
            console.error('[SSEClient] Stream request exception:', errorObj.message);
            this.listener.onError(errorObj.message || 'Network request failed');
        } finally {
            this.close();
        }
    }

    public close(): void {
        if (this.session) {
            try {
                this.session.close();
            } catch (e) {
                // 忽略关闭错误
            }
            this.session = null;
        }
    }
}
```

**重要说明 - 错误码 1007900992**:

根据 CLAUDE.md 文档，这是鸿蒙 RCP 库中的 "Request canceled" 错误，属于正常行为，不需要当作错误处理。

---

## 三、聊天模块详解

### 3.1 聊天服务 (ChatStreamService)

**文件位置**: `features/chat/service/ChatStreamService.ets`

```typescript
import { SSEClient } from '../../../core/http/SSEClient';
import { toApiUrl } from '../../../core/http/ApiConfig';
import { AiChatRequest } from '../types';
import { AiChatRequestMapper } from '../request/AiChatRequestMapper';

class ChatStreamService {
    async sendStreamMessage(
        req: AiChatRequest,
        onMessage: (text: string) => void,
        onDone: () => void,
        onError: (err: string) => void,
        onPlan?: (text: string) => void,
        onIntent?: (intent: string) => void,
        onAppInfo?: (appName?: string, appDescription?: string) => void,
        onLogoGenerated?: (logoUrl: string) => void,
        onAppGenerated?: (url: string, appName?: string, 
                         appDescription?: string, logoUrl?: string) => void
    ): Promise<void> {
        console.info('[ChatStreamService] ======== Send Message ========');

        // 验证请求
        const validation = AiChatRequestMapper.validate(req);
        if (!validation.valid) {
            onError(validation.error || 'Invalid request');
            return;
        }

        // 转换为 DTO
        const dto = AiChatRequestMapper.toDTO(req);
        if (req.sessionId) {
            dto.sessionId = req.sessionId;
        }

        const url = toApiUrl('/api/ai/agent/unified');

        // 创建 SSE 客户端并连接
        const client = new SSEClient({
            onMessage: onMessage,
            onError: onError,
            onDone: onDone,
            onPlan: onPlan,
            onIntent: onIntent,
            onAppInfo: onAppInfo,
            onLogoGenerated: onLogoGenerated,
            onAppGenerated: onAppGenerated
        });

        await client.connect(url, dto);
    }
}

export const chatStreamService = new ChatStreamService();
```

### 3.2 聊天面板 (ChatPanel)

**文件位置**: `features/chat/components/ChatPanel.ets`

**核心功能**:

1. 显示聊天消息列表
2. 处理用户输入
3. 实时显示 AI 响应（流式）
4. 处理各种 SSE 事件

```typescript
@Component
export struct ChatPanel {
    @Prop topPadding?: number;
    @Prop bottomPadding?: number;
    @Prop @Watch('onSessionIdChange') sessionId: string = '';
    @State messages: ChatMessage[] = [];
    @State inputText: string = '';
    @State isSending: boolean = false;
    private scroller: Scroller = new Scroller();

    aboutToAppear() {
        this.loadHistory();
    }

    onSessionIdChange() {
        this.isSending = false;
        this.loadHistory();
    }

    async loadHistory() {
        if (!this.sessionId) {
            this.messages = [];
            return;
        }

        // 加载历史消息
        const result = await sessionService.getSessionMessages(this.sessionId);
        if (result.success && result.data) {
            this.messages = result.data.map((msg: ApiChatMessage) => 
                mapSessionMessageToChatMessage(msg));
            setTimeout(() => {
                this.scrollToBottom();
            }, 100);
        }
    }

    private async send(): Promise<void> {
        const content = this.inputText.trim();
        if (content.length === 0 || this.isSending) {
            return;
        }
        this.isSending = true;

        // 添加用户消息
        this.messages.push(createUserChatMessage(Date.now(), content));
        this.inputText = '';

        // 添加助手消息占位符
        const botMsgId = Date.now() + 1;
        this.messages.push(createAssistantPlaceholderMessage(botMsgId));
        this.scrollToBottom();

        // 创建草稿状态
        const draft = new ChatStreamDraft();
        let lastUpdateTime = 0;
        const UPDATE_INTERVAL = 100;

        const req = new AiChatRequestModel(content, this.sessionId);

        // 同步消息状态
        const syncMessageState = (force: boolean) => {
            const now = Date.now();
            if (force || now - lastUpdateTime >= UPDATE_INTERVAL) {
                if (applyDraftSnapshot(this.messages, botMsgId, draft)) {
                    lastUpdateTime = now;
                    this.scrollToBottom();
                }
            }
        };

        try {
            await chatStreamService.sendStreamMessage(
                req,
                // onMessage - 处理流式消息
                (text: string) => {
                    draft.appendTextChunk(text);
                    syncMessageState(false);
                },
                // onDone - 流结束
                () => {
                    this.isSending = false;
                    syncMessageState(true);
                    if (finalizeDraftMessage(this.messages, botMsgId, draft)) {
                        this.scrollToBottom();
                    }
                },
                // onError - 错误处理
                (err: string) => {
                    this.isSending = false;
                    if (applyStreamError(this.messages, botMsgId, err)) {
                        this.scrollToBottom();
                    }
                },
                // onPlan - 计划内容
                (text: string) => {
                    draft.appendPlanChunk(text);
                    syncMessageState(false);
                },
                // onIntent - 意图
                (intent: string) => {
                    draft.setIntent(intent);
                },
                // onAppInfo - 应用信息
                (appName?: string, appDescription?: string) => {
                    if (applyAppInfo(this.messages, botMsgId, appName, appDescription)) {
                        this.scrollToBottom();
                    }
                },
                // onLogoGenerated - Logo 生成
                (logoUrl: string) => {
                    if (applyLogoUrl(this.messages, botMsgId, logoUrl)) {
                        this.scrollToBottom();
                    }
                },
                // onAppGenerated - 应用生成
                (previewUrl: string, appName?: string, 
                 appDescription?: string, logoUrl?: string) => {
                    if (applyGeneratedApp(this.messages, botMsgId, previewUrl, 
                                         appName, appDescription, logoUrl)) {
                        this.scrollToBottom();
                    }
                }
            );
        } catch (error) {
            this.isSending = false;
            if (applyStreamException(this.messages, botMsgId, error as Object)) {
                this.scrollToBottom();
            }
        }
    }

    scrollToBottom() {
        this.scroller.scrollEdge(Edge.Bottom);
    }

    build() {
        Column() {
            // 消息列表
            Scroll(this.scroller) {
                Column() {
                    // AI 提示
                    Text('内容由AI生成')
                        .fontSize(12)
                        .fontColor('#999999')
                        .width('100%')
                        .textAlign(TextAlign.Center)
                        .margin({ bottom: 16 })

                    // 消息项
                    ForEach(this.messages, (m: ChatMessage) => {
                        MessageItem({ message: m })
                    }, (m: ChatMessage) => m.id.toString())
                }
                .width('100%')
                .padding({ left: 16, right: 16, top: 16, bottom: 16 })
            }
            .layoutWeight(1)
            .edgeEffect(EdgeEffect.Spring)
            .align(Alignment.Top)

            // 输入区域
            Column() {
                Row() {
                    TextInput({ placeholder: '输入你的想法...', text: this.inputText })
                        .layoutWeight(1)
                        .height(40)
                        .backgroundColor('#F5F5F5')
                        .borderRadius(10)
                        .padding({ left: 16, right: 16 })
                        .onChange((v: string) => this.inputText = v)

                    Button('发送')
                        .height(40)
                        .backgroundColor($r('sys.color.ohos_id_color_emphasize'))
                        .borderRadius(10)
                        .margin({ left: 8 })
                        .enabled(!this.isSending && this.inputText.trim().length > 0)
                        .onClick(() => { this.send(); })
                }
                .width('100%')
            }
            .width('100%')
            .padding({ 
                left: 16, 
                right: 16, 
                bottom: (this.bottomPadding ?? 8) + 5, 
                top: this.topPadding ?? 8 
            })
            .backgroundColor(Color.White)
        }
        .width('100%')
        .height('100%')
    }
}
```

### 3.3 消息状态管理

**文件位置**: `features/chat/state/ChatMessageState.ets`

**状态创建函数**:

```typescript
// 创建用户消息
export function createUserChatMessage(id: number, content: string): ChatMessage {
    return {
        id,
        role: 'user',
        type: 'text',
        content,
        timestamp: id
    };
}

// 创建助手占位符消息
export function createAssistantPlaceholderMessage(id: number): ChatMessage {
    return {
        id,
        role: 'assistant',
        type: 'text',
        content: '',
        isStreaming: true,
        timestamp: id
    };
}

// 应用草稿快照
export function applyDraftSnapshot(
    messages: ChatMessage[], 
    botMsgId: number, 
    draft: ChatStreamDraft
): boolean {
    const msgIndex = messages.findIndex(m => m.id === botMsgId);
    if (msgIndex === -1) return false;
    
    const msg = messages[msgIndex];
    msg.content = draft.getTextContent();
    msg.planContent = draft.getPlanContent();
    msg.intent = draft.getIntent();
    msg.isStreaming = true;
    
    return true;
}

// 完成草稿消息
export function finalizeDraftMessage(
    messages: ChatMessage[], 
    botMsgId: number, 
    draft: ChatStreamDraft
): boolean {
    const msgIndex = messages.findIndex(m => m.id === botMsgId);
    if (msgIndex === -1) return false;
    
    const msg = messages[msgIndex];
    msg.content = draft.getTextContent();
    msg.planContent = draft.getPlanContent();
    msg.intent = draft.getIntent();
    msg.isStreaming = false;
    
    return true;
}

// 应用应用信息
export function applyAppInfo(
    messages: ChatMessage[], 
    botMsgId: number, 
    appName?: string, 
    appDescription?: string
): boolean {
    const msgIndex = messages.findIndex(m => m.id === botMsgId);
    if (msgIndex === -1) return false;
    
    messages[msgIndex].appName = appName;
    messages[msgIndex].appDescription = appDescription;
    
    return true;
}

// 应用生成的 App
export function applyGeneratedApp(
    messages: ChatMessage[], 
    botMsgId: number, 
    previewUrl: string, 
    appName?: string, 
    appDescription?: string, 
    logoUrl?: string
): boolean {
    const msgIndex = messages.findIndex(m => m.id === botMsgId);
    if (msgIndex === -1) return false;
    
    const msg = messages[msgIndex];
    msg.appName = appName;
    msg.appDescription = appDescription;
    msg.previewUrl = previewUrl;
    msg.logoUrl = logoUrl;
    msg.type = 'app';
    
    return true;
}
```

---

## 四、工作区模块详解

### 4.1 工作区屏幕 (WorkspaceScreen)

**文件位置**: `features/workspace/pages/WorkspaceScreen.ets`

**职责**: 应用的主界面，包含顶部栏、内容区域和侧边栏。

```typescript
@ComponentV2
export struct WorkspaceScreen {
    topRectHeight: number = 0;
    bottomRectHeight: number = 0;
    @Local showSideBar: boolean = false;
    @Local currentSessionId: string = '';
    @Local activeTabIndex: number = 0;

    private loginController: LoginController = new LoginController();
    private promptAction?: PromptAction;

    aboutToAppear(): void {
        // 读取安全区域高度
        const GeneratedDestructArray_1 = readSafeAreaHeights();
        this.topRectHeight = GeneratedDestructArray_1[0];
        this.bottomRectHeight = GeneratedDestructArray_1[1];

        // 设置未授权回调
        setOnUnauthorizedCallback(() => {
            console.info('[Index] 收到未授权回调，弹出登录弹窗');
            this.openLoginSheet();
        });

        // 检查登录状态
        if (!authService.isLoggedIn()) {
            console.info('[Index] 未检测到token，弹出登录弹窗');
            setTimeout(() => {
                this.openLoginSheet();
            }, 500);
        } else {
            console.info('[Index] 已检测到token，跳过登录');
            this.initSessions();
        }
    }

    async initSessions() {
        const result = await sessionService.getUserSessions();
        if (result.success && result.data) {
            this.currentSessionId = resolveInitialSessionId(
                result.data.map(session => session.sessionId));
        }
    }

    // 构建顶部栏
    @Builder
    buildTopBar() {
        Row() {
            // 侧边栏按钮
            Button() {
                SymbolGlyph($r('sys.symbol.open_sidebar'))
                    .fontWeight(FontWeight.Normal)
                    .fontSize(22)
                    .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
            }
            .backgroundColor(Color.Transparent)
            .height(40)
            .width(40)
            .onClick(() => {
                this.showSideBar = true;
            })

            Blank().layoutWeight(1)

            // 标签切换
            Row({ space: 8 }) {
                Text('源创')
                    .fontSize(17)
                    .fontWeight(this.activeTabIndex === 0 ? FontWeight.Medium : FontWeight.Regular)
                    .fontColor(this.activeTabIndex === 0 ? '#111111' : '#999999')
                    .onClick(() => {
                        this.activeTabIndex = 0;
                    })

                Text('|')
                    .fontSize(15)
                    .fontColor('#C7C7CC')

                Text('元梦')
                    .fontSize(17)
                    .fontWeight(this.activeTabIndex === 1 ? FontWeight.Medium : FontWeight.Regular)
                    .fontColor(this.activeTabIndex === 1 ? '#111111' : '#999999')
                    .onClick(() => {
                        this.activeTabIndex = 1;
                    })
            }
            .padding({ left: 4, right: 4, top: 6, bottom: 6 })

            Blank().layoutWeight(1)

            // 菜单按钮
            BottomSheetMenu({
                onMenuItemClick: (menuId: string) => {
                    this.handleMenuClick(menuId);
                }
            })
        }
        .width('100%')
        .padding({
            top: this.getUIContext().px2vp(this.topRectHeight),
            left: 16,
            right: 16,
            bottom: 8
        })
    }

    // 构建内容区
    @Builder
    buildContent() {
        Column() {
            this.buildTopBar();

            if (this.activeTabIndex === 0) {
                // 聊天面板
                ChatPanel({
                    bottomPadding: this.getUIContext().px2vp(this.bottomRectHeight) + 5,
                    sessionId: this.currentSessionId
                })
                    .width('100%')
                    .layoutWeight(1);
            } else {
                // 元梦工作区占位符
                YuanDreamWorkspacePlaceholder({
                    bottomPadding: this.getUIContext().px2vp(this.bottomRectHeight)
                })
                    .width('100%')
                    .layoutWeight(1);
            }
        }
        .width('100%')
        .height('100%')
    }

    build() {
        Stack() {
            // 会话侧边栏
            SessionSidebar({
                isShowSidebar: this.showSideBar,
                onSideBarChange: (isShow: boolean) => {
                    this.showSideBar = isShow;
                },
                topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
                selectedSessionId: this.currentSessionId,
                onSessionChange: (sessionId: string) => {
                    this.currentSessionId = sessionId;
                },
                contentBuilder: () => {
                    this.buildContent();
                }
            })
        }
        .width('100%')
        .height('100%')
        .onAppear(() => {
            this.promptAction = this.getUIContext().getPromptAction();
        })
    }
}
```

### 4.2 登录处理

**登录流程**:

1. 检测 token 是否存在
2. 如果不存在，弹出登录弹窗
3. 支持华为账号登录和后备登录方式
4. 登录成功后初始化会话

```typescript
private handleLoginSuccess = (loginType: LoginChannelType): void => {
    console.info('[Index] 登录成功，登录类型:', loginType);
    this.initSessions();
}

private handleLoginFail = async (loginChannelType: LoginChannelType, error: LoginFailError) => {
    console.info('[Index] 登录失败，尝试后端登录');
    
    if (loginChannelType === LoginChannelType.HUAWEI) {
        const result = await authService.login();
        
        if (result.success) {
            this.loginController.clearLoginInfo();
            LoginSheetUtils.close();
            this.handleLoginSuccess(LoginChannelType.HUAWEI);
        }
    }
}
```

---

## 五、会话模块详解

### 5.1 会话服务 (SessionService)

**文件位置**: `features/session/service/SessionService.ets`

```typescript
class SessionService {
    async getUserSessions(): Promise<Result<Session[]>> {
        // 获取用户所有会话
    }
    
    async getSessionMessages(sessionId: string): Promise<Result<SessionMessage[]>> {
        // 获取会话消息
    }
    
    async createSession(title: string): Promise<Result<Session>> {
        // 创建会话
    }
    
    async deleteSession(sessionId: string): Promise.Result {
        // 删除会话
    }
}

export const sessionService = new SessionService();
```

### 5.2 会话侧边栏 (SessionSidebar)

**文件位置**: `features/session/components/SessionSidebar.ets`

**功能**:

- 显示会话列表
- 创建新会话
- 切换会话
- 删除会话

---

## 六、应用中心模块详解

### 6.1 应用商店 (AppStoreScreen)

**文件位置**: `features/app-center/pages/AppStoreScreen.ets`

**功能**:

- 浏览已发布应用
- 搜索应用
- 查看应用详情

### 6.2 我的应用 (MyAppsScreen)

**文件位置**: `features/app-center/pages/MyAppsScreen.ets`

**功能**:

- 显示用户创建的应用列表
- 管理应用（编辑、删除）
- 发布/取消发布应用

### 6.3 应用预览 (PreviewScreen)

**文件位置**: `features/app-center/pages/PreviewScreen.ets`

**功能**:

- 预览生成的 Web 应用
- 支持内嵌预览和外部打开

---

# 第三部分：关键技术问题与解决方案

## 一、后端关键技术问题

### 1.1 SSE 流式响应问题

**问题描述**: SSE 需要保持连接打开，传统的 HTTP 响应模式无法满足需求。

**解决方案**: 使用 Spring WebFlux 的 `Flux<ServerSentEvent<T>>` 返回流式响应。

```java
@PostMapping(value = "/unified")
public Flux<ServerSentEvent<String>> unified(...) {
    return unifiedOrchestrator.handleRequest(request, userId);
}
```

### 1.2 JWT 手动验证问题

**问题描述**: `/api/ai/agent/**` 端点需要手动验证 JWT，而不是依赖 Spring Security 的自动验证。

**解决方案**: 在控制器中手动调用验证逻辑：

```java
AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
String token = authHeader.substring(7);
Long userId = jwtTokenProvider.getUserIdFromToken(token);
```

### 1.3 文件安全存储问题

**问题描述**: 需要防止目录遍历攻击，确保文件操作安全。

**解决方案**: 在 StorageService 中进行路径验证：

```java
Path destinationFile = this.rootLocation.resolve(relativePath).normalize();
if (!destinationFile.startsWith(this.rootLocation)) {
    throw new SecurityException("Cannot store file outside current directory.");
}
```

### 1.4 应用版本管理问题

**问题描述**: 需要支持应用的多版本管理，包括版本创建、切换和删除。

**解决方案**: 使用 AppVersionEntity 存储每个版本的代码，通过 currentVersionId 指向最新版本。

### 1.5 模板匹配响应延迟问题

**问题描述**: 当模板匹配成功时，需要让用户感知到匹配结果，但又要保持流式响应的连续性。

**解决方案**: 使用可配置的延迟：

```java
private Duration resolveTemplateResponseDelay(String matchedTemplate) {
    if (matchedTemplate == null) return Duration.ZERO;
    return Duration.ofMillis(templateMatchedResponseDelayMs);
}
```

---

## 二、鸿蒙端关键技术问题

### 2.1 SSE 文本解码问题

**问题描述**: SSE 流可能包含多字节 UTF-8 字符，需要正确处理。

**解决方案**: 使用流模式解码：

```typescript
const decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
const chunk = this.decoder.decodeToString(new Uint8Array(buffer), { stream: true });
```

### 2.2 请求取消错误码处理

**问题描述**: 错误码 1007900992 表示 "Request canceled"，是正常行为。

**解决方案**: 在错误处理中忽略此错误：

```typescript
if (errorObj.code === 1007900992) {
    console.info('[SSEClient] Request canceled by client (Normal termination)');
    return;
}
```

### 2.3 JSON.parse 类型问题

**问题描述**: ArkTS 中 JSON.parse 返回 unknown 类型，需要显式类型转换。

**解决方案**: 使用类型断言或 Record 类型：

```typescript
const parsed: Record<string, Object> = JSON.parse(eventData);
```

### 2.4 安全区域适配问题

**问题描述**: 需要处理不同设备的刘海屏和底部安全区域。

**解决方案**: 使用 readSafeAreaHeights 获取安全区域高度，并进行转换：

```typescript
const topRectHeight = GeneratedDestructArray_1[0];
const bottomRectHeight = GeneratedDestructArray_1[1];
this.getUIContext().px2vp(this.topRectHeight)
```

### 2.5 未授权回调处理问题

**问题描述**: 当收到 401 响应时，需要自动弹出登录界面。

**解决方案**: 使用全局回调机制：

```typescript
setOnUnauthorizedCallback(() => {
    this.openLoginSheet();
});
```

---

# 第四部分：数据流与架构总结

## 一、系统数据流

### 1.1 用户请求处理流程

```
用户输入
    │
    ▼
[鸿蒙端] ChatPanel
    │
    ▼
SSEClient.connect()
    │
    ▼
POST /api/ai/agent/unified
    │
    ▼
[后端] UnifiedController
    │
    ▼
UnifiedOrchestrator.handleRequest()
    │
    ├──► IntentAnalyzer.analyze()
    │       │
    │       ▼
    │   意图分类 (CHAT/GEN/EDIT)
    │
    └──► 执行对应的 Pipeline
            │
            ├── CHAT → ChatPipelineService
            ├── GEN  → AppGenPipelineService
            └── EDIT → AppEditPipelineService
                    │
                    ▼
            SSE 流式响应
                    │
                    ▼
            [鸿蒙端] SSEListener 回调
                    │
                    ▼
            更新 UI
```

### 1.2 应用生成流程

```
用户请求: "帮我创建一个计算器"
    │
    ▼
意图识别: GEN
    │
    ├─► 模板匹配 (TemplateMatcherService)
    ├─► AI 对话 (ChatAgent.chatBeforeGen)
    ├─► 计划生成 (PlanGenerator)
    └─► 应用信息提取 (AppInfoExtractor)
            │
            ▼
    创建 App 记录 (AppService.createApp)
            │
            ▼
    生成 Logo (ImageService.generateLogoAndSave)
            │
            ▼
    创建版本 (AppVersionService.createVersion)
            │
            ├─► 有模板 → 复制模板文件
            └─► 无模板 → OpenCode 生成
            │
            ▼
    返回 SSE: app_generated
```

## 二、模块依赖关系

```
┌─────────────────────────────────────────────────────────────────┐
│                      后端 (Spring Boot)                          │
├─────────────────────────────────────────────────────────────────┤
│  UnifiedController                                              │
│       │                                                         │
│       ▼                                                         │
│  UnifiedOrchestrator ───────┬────────────────────────────────── │
│       │                     │                                  │
│       ├─► IntentAnalyzer   │  (LangChain4j @AiService)         │
│       │                     │                                  │
│       ├─► ChatPipeline    │                                   │
│       │       │            │                                   │
│       │       ▼            │                                   │
│       │   ChatAgent       │  (LangChain4j @AiService)         │
│       │                     │                                  │
│       ├─► AppGenPipeline  │                                   │
│       │       │            │                                   │
│       │       ├─► TemplateMatcherService                      │
│       │       ├─► ChatAgent                                   │
│       │       ├─► PlanGenerator                               │
│       │       ├─► AppInfoExtractor                            │
│       │       ├─► AppService                                  │
│       │       └─► OpenCodeClient                              │
│       │                     │                                  │
│       └─► AppEditPipeline  │                                   │
│               │            │                                   │
│               └─► ...     │                                   │
│                          │                                  │
├──────────────────────────┼──────────────────────────────────────┤
│                          ▼                                       │
│              AppService / AppVersionService                       │
│                          │                                       │
│              ┌────────────┴────────────┐                         │
│              │                         │                         │
│              ▼                         ▼                         │
│    AppApplicationService     AppVersionService                   │
│              │                         │                         │
│              ▼                         ▼                         │
│    AppQueryService          StorageService                       │
│              │                                                      │
│              ▼                                                      │
│    AppRepository / AppVersionRepository                            │
│              │                                                      │
│              ▼                                                      │
│         PostgreSQL                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     鸿蒙端 (HarmonyOS)                           │
├─────────────────────────────────────────────────────────────────┤
│  WorkspaceScreen                                                  │
│       │                                                          │
│       ├─► ChatPanel                                             │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   ChatStreamService                                     │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   SSEClient                                             │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   POST /api/ai/agent/unified                            │
│       │                                                          │
│       ├─► SessionSidebar                                        │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   SessionService                                        │
│       │       │                                                  │
│       │       ▼                                                  │
│       │   GET /api/ai/sessions                                   │
│       │                                                          │
│       └─► AuthService                                           │
│               │                                                  │
│               ▼                                                  │
│           POST /api/auth/login                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

*文档生成时间: 2026-04-19*
*项目: MetaCraft - AI 驱动应用生成平台*
