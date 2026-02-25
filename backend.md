# MetaCraft 后端说明文档

## 项目概述

MetaCraft 后端是一个基于 Spring Boot 3.x 的 AI 驱动应用生成平台，提供智能对话和应用生成功能。核心亮点是通过意图识别自动判断用户是想聊天还是生成应用，并通过 LangChain4j 的 Tool 机制让 AI 能够自动调用后端服务来保存生成的应用。

## 技术栈

### 核心框架
- **Spring Boot**: 3.5.9
- **Java**: 21
- **Spring Security**: 用户认证和授权
- **Spring Data JPA**: 数据持久化
- **Flyway**: 数据库版本管理

### 数据库
- **PostgreSQL**: 主数据库

### AI 集成
- **LangChain4j**: 1.11.0-beta19（核心 AI 框架）
  - langchain4j-reactor: 响应式流式支持
  - langchain4j-community-dashscope-spring-boot-starter: 通义千问集成
  - langchain4j-spring-boot-starter: Spring Boot 集成

### 工具库
- **Lombok**: 简化 Java 代码
- **JWT (java-jwt)**: 令牌认证
- **SpringDoc OpenAPI**: API 文档 (Swagger UI)

## 项目结构

```
apps/api/
├── src/main/java/com/metacraft/api/
│   ├── MetaCraftApiApplication.java          # 应用入口
│   ├── config/                                 # 配置类
│   │   ├── SecurityConfig.java               # 安全配置
│   │   ├── WebMvcConfig.java                 # Web MVC 配置
│   │   └── OpenApiConfig.java                # Swagger 配置
│   ├── security/                               # 安全相关
│   │   ├── JwtTokenProvider.java             # JWT 令牌生成
│   │   ├── JwtAuthenticationFilter.java      # JWT 认证过滤器
│   │   ├── UserDetailsServiceImpl.java       # 用户详情服务
│   │   ├── CustomUserDetails.java            # 自定义用户详情
│   │   └── AuthUtils.java                    # 认证工具类
│   ├── exception/                              # 异常处理
│   │   ├── GlobalExceptionHandler.java       # 全局异常处理器
│   │   └── UnauthorizedException.java        # 未授权异常
│   ├── response/                               # 响应封装
│   │   ├── ApiResponse.java                  # API 响应结构
│   │   └── Response.java                     # 响应构建器
│   └── modules/                                # 业务模块
│       ├── user/                              # 用户模块
│       │   ├── controller/
│       │   │   ├── AuthController.java       # 认证接口
│       │   │   └── UserController.java       # 用户管理接口
│       │   ├── dto/                           # 数据传输对象
│       │   ├── vo/                            # 视图对象
│       │   └── service/
│       │       └── UserService.java          # 用户服务
│       ├── ai/                                # AI 模块（核心）
│       │   ├── controller/
│       │   │   ├── AgentController.java      # AI 智能体统一入口
│       │   │   ├── ChatSessionController.java # 聊天会话接口
│       │   │   └── ChatMessageController.java # 聊天消息接口
│       │   ├── dto/                           # AI 相关 DTO
│       │   │   ├── AgentRequestDTO.java      # 智能体请求
│       │   │   ├── AgentIntentRequestDTO.java # 意图识别请求
│       │   │   ├── AppMetadataDTO.java       # 应用元数据
│       │   │   ├── ChatRequestDTO.java       # 聊天请求
│       │   │   ├── ChatSessionCreateDTO.java # 会话创建
│       │   │   ├── ChatSessionUpdateDTO.java # 会话更新
│       │   │   ├── ChatMessageCreateDTO.java # 消息创建
│       │   │   └── GenerateLogoRequestDTO.java # Logo 生成请求
│       │   ├── entity/                        # AI 相关实体
│       │   │   ├── ChatSessionEntity.java    # 聊天会话实体
│       │   │   └── ChatMessageEntity.java    # 聊天消息实体
│       │   ├── repository/                    # 数据访问层
│       │   │   ├── ChatSessionRepository.java
│       │   │   └── ChatMessageRepository.java
│       │   ├── service/                       # 业务逻辑层
│       │   │   ├── AgentService.java         # 智能体统一服务（意图识别 + 路由）
│       │   │   ├── AgentIntentService.java   # 意图识别服务
│       │   │   ├── AgentAiService.java       # AI 服务接口（@AiService）
│       │   │   ├── AgentToolService.java     # AI 工具服务（@Tool 定义）
│       │   │   ├── ChatSessionService.java   # 聊天会话服务
│       │   │   └── ChatMessageService.java   # 聊天消息服务
│       │   ├── vo/                            # 视图对象
│       │   │   ├── AgentResponseVO.java
│       │   │   ├── ChatResponseVO.java
│       │   │   ├── ChatSessionVO.java
│       │   │   ├── ChatMessageVO.java
│       │   │   ├── PlanResponseVO.java
│       │   │   └── GenerateLogoResponseVO.java
│       │   └── config/
│       │       └── AiConfig.java             # AI 配置（目前为空）
│       ├── app/                               # 应用模块
│       │   ├── controller/
│       │   │   └── PreviewController.java    # 应用预览接口
│       │   ├── entity/
│       │   │   ├── AppEntity.java            # 应用实体
│       │   │   └── AppVersionEntity.java     # 应用版本实体
│       │   ├── repository/
│       │   │   ├── AppRepository.java
│       │   │   └── AppVersionRepository.java
│       │   └── service/
│       │       └── AppService.java          # 应用管理服务
│       └── storage/                           # 存储模块
│           └── service/
│               └── StorageService.java       # 文件存储服务
└── src/main/resources/
    ├── application.yaml                        # 应用配置
    ├── db/migration/                          # 数据库迁移脚本
    │   ├── V1__init_base.sql                 # 基础函数
    │   ├── V2__create_user_tables.sql        # 用户表
    │   ├── V3__create_apps_and_versions_table.sql # 应用表
    │   ├── V4__create_chat_sessions_table.sql # 会话表
    │   └── V5__create_chat_messages_table.sql # 消息表
    └── prompts/                               # AI 提示词模板
        ├── agent-system.txt                  # 通用系统提示词
        ├── agent-chat.txt                    # 聊天系统提示词
        ├── agent-gen-app.txt                 # 应用生成系统提示词
        ├── intent-classification.txt         # 意图识别提示词
        └── image-logo-gen.txt                # Logo 生成提示词
```

## 核心功能架构

### 1. 智能体统一入口

MetaCraft 的核心是 `AgentController` 提供的统一入口，通过 SSE（Server-Sent Events）流式返回 AI 响应。

**接口**: `POST /api/ai/agent/unified`

**工作流程**:

```
用户输入
    ↓
AgentService.unified()
    ↓
AgentIntentService.classifyIntent()  ← 意图识别（LangChain4j）
    ↓
判断意图:
    ├─ "chat" → AgentAiService.chat()  → 普通对话
    └─ "gen"  → AgentAiService.generateApp() → 调用 @Tool 保存应用
    ↓
SSE 流式返回:
    └─ event: intent + event: message
```

### 2. 意图识别服务

**文件**: `AgentIntentService.java`

使用 LangChain4j 的 `ChatModel` 进行意图分类：

```java
public String classifyIntent(AgentIntentRequestDTO request) {
    PromptTemplate promptTemplate = PromptTemplate.from(INTENT_PROMPT_TEMPLATE);
    Prompt prompt = promptTemplate.apply(Map.of("message", request.getMessage()));
    String response = chatModel.chat(prompt.text());
    
    String intent = response.trim().toLowerCase();
    return intent.contains("gen") ? "gen" : "chat";
}
```

**意图类型**:
- `chat`: 用户想聊天、提问或讨论一般话题
- `gen`: 用户想生成网页、应用、工具或代码

### 3. AI 服务接口（LangChain4j 核心）

**文件**: `AgentAiService.java`

使用 LangChain4j 的 `@AiService` 注解定义 AI 服务接口，LangChain4j 会自动实现这个接口：

```java
@AiService
public interface AgentAiService {

    @SystemMessage(fromResource = "prompts/agent-chat.txt")
    Flux<String> chat(@UserMessage String message);

    @SystemMessage(fromResource = "prompts/agent-gen-app.txt")
    Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
}
```

**关键特性**:
- 使用响应式编程（`Flux<String>`）实现流式输出
- `@SystemMessage` 注解指定系统提示词（从文件加载）
- `@UserMessage` 注解标记用户消息参数
- `@V("userId")` 注解用于传递变量到提示词模板
- LangChain4j 自动生成实现类，处理与 LLM 的交互

### 4. AI 工具服务

**文件**: `AgentToolService.java`

使用 LangChain4j 的 `@Tool` 注解定义可被 AI 调用的工具：

```java
@Service
public class AgentToolService {

    @Tool("Save the generated application code, name, and description. Returns the preview URL.")
    public String saveApp(
            @P("The name of the application") String name,
            @P("A short description of the application") String description,
            @P("The complete HTML source code of the application") String code,
            @P("The user ID provided in the context") Long userId
    ) {
        AppEntity app = appService.createApp(userId, name, description);
        appService.createVersion(app.getId(), code, "Initial generation by AI");
        return "/api/preview/" + app.getUuid() + "/v/1";
    }
}
```

**关键特性**:
- `@Tool` 注解定义工具功能描述
- `@P` 注解标记参数描述，帮助 AI 理解如何调用
- LangChain4j 自动将此工具注册到 `generateApp()` 使用的 AI agent 中
- AI 在生成应用代码后，会自动调用 `saveApp()` 工具保存应用
- 返回预览 URL 供用户访问

### 5. 应用生成流程

**提示词模板** (`agent-gen-app.txt`):

```
You are an expert full-stack developer capable of generating web applications.
Your goal is to help the user create a complete, single-file HTML application.

Current User ID: {{userId}}

Process:
1. Analyze the user's request.
2. Briefly explain your plan to the user (streaming).
3. Generate a creative name and description for the app internally.
4. Write the complete, single-file HTML code (including CSS/JS).
   - Use Tailwind CSS via CDN for styling.
   - Use Alpine.js via CDN for interactivity.
5. Call the 'saveApp' tool to save the application.
   - IMPORTANT: You MUST pass the 'userId' provided above ({{userId}}) to the 'saveApp' tool.
6. Provide the returned preview URL to the user.

Constraints:
- Do NOT ask the user for clarification unless absolutely necessary. Make reasonable assumptions.
- Ensure the code is production-ready and bug-free.
```

**流程说明**:
1. 分析用户请求
2. 向用户说明计划（流式输出）
3. 生成应用名称和描述
4. 生成完整的 HTML 代码（包含 CSS/JS）
5. 调用 `saveApp` 工具保存应用（AI 自动调用）
6. 返回预览 URL 给用户

### 6. 应用管理服务

**文件**: `AppService.java`

**核心方法**:

#### 创建应用
```java
public AppEntity createApp(Long userId, String name, String description) {
    AppEntity app = AppEntity.builder()
            .userId(userId)
            .name(name)
            .uuid(UUID.randomUUID().toString())
            .description(description)
            .isPublic(false)
            .build();
    return appRepository.save(app);
}
```

#### 创建版本
```java
public AppVersionEntity createVersion(Long appId, String htmlContent, String changeLog) {
    AppEntity app = appRepository.findById(appId).orElseThrow(...);
    
    Integer nextVersion = appVersionRepository.findTopByAppIdOrderByVersionNumberDesc(appId)
            .map(v -> v.getVersionNumber() + 1)
            .orElse(1);
    
    String relativePath = String.format("apps/%d/v%d/index.html", appId, nextVersion);
    storageService.saveTextFile(relativePath, htmlContent);
    
    AppVersionEntity version = AppVersionEntity.builder()
            .appId(appId)
            .versionNumber(nextVersion)
            .storagePath(relativePath)
            .changeLog(changeLog)
            .build();
    version = appVersionRepository.save(version);
    
    app.setCurrentVersionId(version.getId());
    appRepository.save(app);
    
    return version;
}
```

**版本管理**:
- 每个应用支持多版本（v1, v2, v3...）
- 版本号自动递增
- 代码文件存储在文件系统中
- 数据库记录版本元信息和存储路径

### 7. 文件存储服务

**文件**: `StorageService.java`

**核心功能**:

#### 保存文件
```java
public String saveTextFile(String relativePath, String content) {
    Path destinationFile = this.rootLocation.resolve(relativePath).normalize();
    
    // 安全检查
    if (!destinationFile.startsWith(this.rootLocation)) {
        throw new SecurityException("Cannot store file outside current directory.");
    }
    
    Files.createDirectories(destinationFile.getParent());
    Files.write(destinationFile, content.getBytes(StandardCharsets.UTF_8), 
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    
    return destinationFile.toString();
}
```

#### 读取文件
```java
public String readTextFile(String relativePath) {
    Path file = this.rootLocation.resolve(relativePath).normalize();
    
    if (!file.startsWith(this.rootLocation)) {
        throw new SecurityException("Cannot read file outside current directory.");
    }
    
    return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
}
```

**安全特性**:
- 路径规范化，防止目录遍历攻击
- 路径安全检查，确保文件在存储目录内
- 自动创建父目录

### 8. 应用预览

**文件**: `PreviewController.java`

**接口**:

#### 预览最新版本
```
GET /api/preview/{uuid}
```

#### 预览指定版本
```
GET /api/preview/{uuid}/v/{versionNumber}
```

**流程**:
1. 通过 UUID 查找应用（防止遍历攻击）
2. 获取版本详情
3. 从文件系统读取 HTML 内容
4. 返回 HTML 内容供 Web 渲染

## 数据库设计

### users 表
用户信息表，存储用户基本信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 ID |
| email | TEXT | 用户邮箱，唯一标识 |
| name | TEXT | 用户姓名 |
| password_hash | TEXT | 密码哈希值 |
| avatar_base64 | TEXT | 头像 Base64 编码数据 |
| bio | TEXT | 用户简介 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### apps 表
应用容器表，存储应用元数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 ID |
| uuid | VARCHAR(64) | 对外展示的唯一标识 (UUID) |
| user_id | BIGINT | 用户 ID，外键关联 users 表 |
| name | TEXT | 应用名称 |
| description | TEXT | 应用描述 |
| is_public | BOOLEAN | 是否公开 |
| current_version_id | BIGINT | 当前最新版本 ID，外键关联 app_versions 表 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### app_versions 表
应用版本历史表，存储代码快照。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 ID |
| app_id | BIGINT | 应用 ID，外键关联 apps 表 |
| version_number | INT | 版本号 (1, 2, 3...) |
| storage_path | VARCHAR(512) | 代码文件存储路径 |
| change_log | TEXT | 修改说明 (AI 生成的 commit message) |
| created_at | TIMESTAMPTZ | 创建时间 |

### chat_sessions 表
聊天会话表。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 ID |
| uuid | VARCHAR(64) | 会话唯一标识 (UUID) |
| user_id | BIGINT | 用户 ID，外键关联 users 表 |
| app_id | BIGINT | 应用 ID，外键关联 apps 表 |
| title | TEXT | 会话标题 |
| created_at | TIMESTAMPTZ | 创建时间 |
| updated_at | TIMESTAMPTZ | 更新时间 |

### chat_messages 表
聊天消息表。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 ID |
| session_id | BIGINT | 会话 ID，外键关联 chat_sessions 表 |
| role | VARCHAR(20) | 消息角色 (user/assistant) |
| content | TEXT | 消息内容 |
| created_at | TIMESTAMPTZ | 创建时间 |

## API 接口说明

### 认证接口

#### 用户注册
- **URL**: `POST /api/auth/register`
- **说明**: 创建新用户账号
- **请求体**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123",
    "name": "张三"
  }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "message": "User registered successfully",
    "data": {
      "token": "jwt_token_here",
      "user": {
        "id": 1,
        "email": "user@example.com",
        "name": "张三"
      }
    }
  }
  ```

#### 用户登录
- **URL**: `POST /api/auth/login`
- **说明**: 通过邮箱和密码登录
- **请求体**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **响应**: 同用户注册

### 用户管理接口

#### 获取当前用户信息
- **URL**: `GET /api/user`
- **说明**: 获取当前登录用户信息
- **请求头**: `Authorization: Bearer {token}`

#### 更新用户信息
- **URL**: `PATCH /api/user`
- **说明**: 部分更新用户信息
- **请求头**: `Authorization: Bearer {token}`

### AI 智能体接口（核心）

#### 智能体统一入口
- **URL**: `POST /api/ai/agent/unified`
- **说明**: SSE 流式统一入口，自动识别意图并流式返回
- **请求头**: `Authorization: Bearer {token}`
- **请求体**:
  ```json
  {
    "message": "帮我创建一个记账本应用",
    "sessionId": null
  }
  ```
- **响应**: SSE 流式响应，包含两种事件类型：
  
  **事件 1: intent（意图识别结果）**
  ```
  event: intent
  data: gen
  ```
  
  **事件 2: message（AI 生成的内容流）**
  ```
  event: message
  data: 好的，我来帮你创建一个记账本应用...
  
  event: message
  data: 这是一个完整的单文件 HTML 应用...
  ```

**使用场景**:

1. **聊天场景**:
   ```
   用户: "你好，介绍一下 MetaCraft"
   意图: chat
   响应: "你好！我是元梦（YuanMeng）..."
   ```

2. **生成应用场景**:
   ```
   用户: "帮我创建一个待办事项应用"
   意图: gen
   响应: 
     - 流式输出应用规划
     - 流式生成 HTML 代码
     - 自动调用 saveApp 工具
     - 返回预览 URL
   ```

### 聊天会话接口

#### 创建会话
- **URL**: `POST /api/ai/sessions`
- **说明**: 创建新的聊天会话
- **请求头**: `Authorization: Bearer {token}`

#### 获取用户所有会话
- **URL**: `GET /api/ai/sessions`
- **说明**: 获取当前用户的所有聊天会话
- **请求头**: `Authorization: Bearer {token}`

#### 获取指定会话
- **URL**: `GET /api/ai/sessions/{sessionId}`
- **说明**: 根据 ID 获取聊天会话
- **请求头**: `Authorization: Bearer {token}`

#### 更新会话
- **URL**: `PATCH /api/ai/sessions/{sessionId}`
- **说明**: 更新聊天会话
- **请求头**: `Authorization: Bearer {token}`

#### 删除会话
- **URL**: `DELETE /api/ai/sessions/{sessionId}`
- **说明**: 删除聊天会话
- **请求头**: `Authorization: Bearer {token}`

#### 获取会话消息
- **URL**: `GET /api/ai/sessions/{sessionId}/messages`
- **说明**: 获取聊天会话的所有消息
- **请求头**: `Authorization: Bearer {token}`

### 应用预览接口

#### 预览应用最新版本
- **URL**: `GET /api/preview/{uuid}`
- **说明**: 预览应用的最新版本
- **响应**: HTML 内容

#### 预览应用指定版本
- **URL**: `GET /api/preview/{uuid}/v/{versionNumber}`
- **说明**: 预览应用的指定版本
- **响应**: HTML 内容

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| DB_URL | - | PostgreSQL 数据库连接 URL |
| DB_USERNAME | - | 数据库用户名 |
| DB_PASSWORD | - | 数据库密码 |
| API_KEY | - | 通义千问 API Key |
| MODEL_NAME | qwen-plus | AI 模型名称 |
| JWT_SECRET | change-this-secret-key-in-production-at-least-256-bits | JWT 密钥 |
| JWT_EXPIRATION | 8640000000 | JWT 过期时间 (毫秒) |
| APP_STORAGE_PATH | apps/data | 应用代码存储路径 |
| SERVER_PORT | 8080 | 服务器端口 |

### 应用配置

```yaml
app:
  storage:
    path: ${APP_STORAGE_PATH:apps/data}
  jwt:
    expiration: ${JWT_EXPIRATION:8640000000}
    secret: ${JWT_SECRET:change-this-secret-key-in-production-at-least-256-bits}

spring:
  application:
    name: metacraft-api
  datasource:
    driver-class-name: org.postgresql.Driver
    password: ${DB_PASSWORD}
    url: ${DB_URL}
    username: ${DB_USERNAME}
  flyway:
    baseline-on-migrate: true
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

langchain4j:
  community:
    dashscope:
      chat-model:
        model-name: ${MODEL_NAME:qwen-plus}
        api-key: ${API_KEY}
```

## 安全机制

### JWT 认证
- 使用 JWT (JSON Web Token) 进行用户认证
- 令牌包含用户 ID 和邮箱信息
- 所有需要认证的接口都需要在请求头中携带 `Authorization: Bearer {token}`

### 权限控制
- Spring Security 配置实现了基于角色的访问控制
- 公开接口：
  - `/api/auth/register`
  - `/api/auth/login`
  - `/api/preview/**`
  - `/api/health`
  - `/api/ai/agent/**` (手动验证)
  - Swagger UI 相关路径
- 其他所有接口都需要认证

### 密码加密
- 使用 BCrypt 算法加密存储用户密码
- 密码在传输和存储过程中均经过加密处理

### 文件路径安全
- 路径规范化，防止目录遍历攻击
- 路径安全检查，确保文件在存储目录内

## LangChain4j 集成详解

### 1. @AiService 注解

LangChain4j 的 `@AiService` 注解自动实现接口，处理与 LLM 的交互：

```java
@AiService
public interface AgentAiService {
    @SystemMessage(fromResource = "prompts/agent-gen-app.txt")
    Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
}
```

**工作原理**:
1. LangChain4j 在启动时扫描 `@AiService` 注解的接口
2. 使用配置的 `ChatModel`（通义千问）自动生成实现类
3. 处理系统提示词、用户消息和变量替换
4. 支持流式响应（`Flux<String>`）

### 2. @Tool 注解

LangChain4j 的 `@Tool` 注解将 Java 方法注册为 AI 可调用的工具：

```java
@Tool("Save the generated application code, name, and description. Returns the preview URL.")
public String saveApp(
        @P("The name of the application") String name,
        @P("A short description of the application") String description,
        @P("The complete HTML source code of the application") String code,
        @P("The user ID provided in the context") Long userId
) {
    // 实现逻辑
}
```

**工作原理**:
1. LangChain4j 扫描 `@Tool` 注解的方法
2. 自动将工具描述和参数描述添加到 AI 的系统提示词中
3. AI 在生成过程中根据需要调用工具
4. 工具调用结果自动反馈给 AI

### 3. 变量注入

使用 `@V("variableName")` 注解注入变量到提示词模板：

```java
Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
```

提示词模板中使用：
```
Current User ID: {{userId}}
```

### 4. 响应式流式输出

使用 `Flux<String>` 实现流式输出：

```java
Flux<String> chat(@UserMessage String message);
```

在 Controller 中转换为 SSE：
```java
Flux<ServerSentEvent<String>> messageStream = aiStream
    .map(content -> ServerSentEvent.<String>builder()
            .event("message")
            .data(content)
            .build());
```

## 技术亮点

### 1. 从 Spring AI 迁移到 LangChain4j

项目最初计划使用 Spring AI，但最终迁移到 LangChain4j，主要原因：

- **更简洁的 API**: LangChain4j 的注解方式更直观
- **更好的 Java 集成**: `@AiService` 和 `@Tool` 注解天然支持 Java
- **更灵活的工具系统**: 自动工具注册和调用
- **更好的响应式支持**: 原生支持 `Flux` 流式输出

### 2. 智能意图识别

通过意图识别自动判断用户意图，实现统一的交互入口：
- 无需用户明确说明是聊天还是生成应用
- 提升用户体验，减少操作步骤
- 降低学习成本

### 3. AI 工具调用

LangChain4j 的 `@Tool` 机制让 AI 能够自动调用后端服务：
- AI 可以保存应用、查询数据等
- 无需手动处理复杂的 API 调用逻辑
- 自然语言与代码的无缝集成

### 4. 应用版本管理

完整的应用版本管理系统：
- 支持多版本历史
- 版本快照存储
- 灵活的版本回滚

### 5. 响应式架构

使用 Spring WebFlux 和 Reactor：
- 非阻塞 I/O，提升并发性能
- SSE 流式输出，实时响应
- 更好的资源利用率

## 部署说明

### 开发环境
1. 配置环境变量或修改 `application.yaml`
2. 确保 PostgreSQL 数据库已启动
3. 配置通义千问 API Key
4. 运行应用：`./mvnw spring-boot:run`
5. 访问 Swagger UI: `http://localhost:8080/swagger-ui.html`

### 生产环境
1. 修改 JWT_SECRET 为强随机值（至少 256 位）
2. 配置生产数据库连接
3. 配置正确的 API_KEY
4. 调整存储路径权限
5. 使用反向代理（如 Nginx）进行部署
6. 配置 HTTPS
7. 设置日志级别为 INFO 或 WARN

### Docker 部署（推荐）

创建 `Dockerfile`:
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY target/api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行：
```bash
./mvnw clean package
docker build -t metacraft-api .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://db:5432/metacraft \
  -e DB_USERNAME=metacraft \
  -e DB_PASSWORD=password \
  -e API_KEY=your_api_key \
  metacraft-api
```

## 常见问题

### 1. 意图识别不准确怎么办？
调整 `AgentIntentService` 中的提示词模板，优化分类逻辑。

### 2. AI 生成的应用代码不完整？
检查 `agent-gen-app.txt` 提示词，添加更详细的约束和要求。

### 3. 工具调用失败？
确保 `AgentToolService` 的方法使用正确的 `@Tool` 和 `@P` 注解，检查参数描述是否清晰。

### 4. 文件存储路径错误？
检查 `APP_STORAGE_PATH` 环境变量配置，确保目录存在且有写权限。

### 5. SSE 连接断开？
检查 Nginx 或反向代理配置，确保支持 SSE 长连接。

## 注意事项

1. **安全性**: 生产环境务必修改默认的 JWT_SECRET
2. **API Key**: 确保 API_KEY 安全，不要泄露到公开仓库
3. **数据库备份**: 定期备份 PostgreSQL 数据库
4. **日志监控**: 生产环境应配置日志收集和监控
5. **性能优化**: 根据实际负载调整 JVM 参数和数据库连接池配置
6. **版本控制**: 建议使用 Git 管理提示词模板，便于追踪和优化
7. **测试覆盖**: 为 AI 相关功能编写测试用例，确保稳定性

## 扩展建议

### 1. 增强意图识别
- 添加更多意图类型（如：查询、修改、删除）
- 使用更复杂的分类模型
- 支持多意图混合

### 2. 丰富工具库
- 添加应用修改工具
- 添加数据导入/导出工具
- 添加应用发布工具

### 3. 优化提示词
- 使用 Few-shot Learning 提升生成质量
- 添加代码示例和模板
- 支持自定义风格和主题

### 4. 增强版本管理
- 支持版本比较
- 支持版本回滚
- 支持版本分支

### 5. 性能优化
- 缓存常用应用代码
- 异步处理文件 I/O
- 使用 CDN 加速静态资源
