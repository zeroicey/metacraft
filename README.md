# MetaCraft

MetaCraft 是一个 AI 驱动的应用生成平台。用户通过自然语言描述需求，系统可以完成对话、生成方案、产出可运行网页代码、保存版本，并通过 SSE 实时回传生成过程。

English README: `docs/README.en.md`

## 仓库内容

- `apps/api`: Spring Boot 后端（AI 编排、认证、应用版本管理、SSE 流式接口）
- `apps/huawei`: HarmonyOS 客户端（ArkTS/ETS）
- `docs`: 补充文档（架构说明、英文 README 等）

## 核心能力

- 统一 AI 入口，按意图路由到 `chat`、`gen`、`edit` 流水线
- 实时 SSE 事件流，前端可边生成边展示
- 会话与消息持久化（含应用关联字段）
- 应用与版本生命周期管理（数据库 + 文件系统）
- Prompt 和工具链可持续迭代，便于能力扩展

## 系统架构

请求主路径：`POST /api/ai/agent/unified`

1. 客户端发起统一请求
2. 后端校验用户身份
3. `UnifiedOrchestrator` 处理/创建会话并落库用户消息
4. 意图识别后路由到对应 pipeline
5. pipeline 通过 SSE 持续输出事件并在结束后持久化结果

### 当前 SSE 事件

| 事件 | 说明 |
| --- | --- |
| `intent` | 意图识别结果 |
| `message` | AI 文本流片段 |
| `plan` | 生成方案片段 |
| `app_info` | 抽取到的应用信息 |
| `logo_generated` | logo 生成完成 |
| `app_generated` | 应用生成并可预览 |
| `error` | 错误事件 |

## 目录结构

```text
metacraft/
|- apps/
|  |- api/
|  |  |- src/main/java/com/metacraft/api/
|  |  |  |- config/
|  |  |  |- modules/
|  |  |     |- ai/
|  |  |     |- app/
|  |  |     |- user/
|  |  |- src/main/resources/
|  |     |- db/migration/
|  |     |- prompts/
|  |     |- public/
|  |- huawei/
|- docs/
|- README.md
```

## 技术栈

### 后端（`apps/api`）

- Java 21
- Spring Boot 3.5.x
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Flyway
- Reactor (`Flux`) + SSE
- LangChain4j
- Swagger/OpenAPI (`springdoc`)

### 前端（`apps/huawei`）

- HarmonyOS（ArkTS/ETS）
- OHPM 依赖管理
- 自定义 SSE 客户端（基于 `@kit.RemoteCommunicationKit`）
- AGCIT 公共模块：`aggregated_login`、`collect_personal_info`、`app_setting`

## HarmonyOS 前端说明

`apps/huawei` 采用分层结构，便于页面开发、API 维护和网络能力复用。

### 前端结构

- `AppScope`: 应用级配置（bundle、图标、label）
- `entry`: 主模块
- `entry/src/main/ets/pages`: 页面容器（`Index`、`Explore`、`Preview`、`Profile`、`Setting` 等）
- `entry/src/main/ets/components`: 复用组件（`ChatPanel`、`AppPreviewCard`、侧边栏等）
- `entry/src/main/ets/api`: 业务 API 封装（`auth`、`aiChat`、`chatSession`、`appApi`）
- `entry/src/main/ets/utils/http`: 网络层（`HttpManager`、拦截器、`SSEClient`、`ApiConfig`）
- `entry/src/main/ets/model`: 类型与模型转换
- `ohos_agcit`: 登录、隐私、设置等公共能力

### 前端运行链路

1. `Index.ets` 挂载核心聊天组件 `ChatPanel`
2. 启动时检查登录态并注册未授权回调
3. 调用 `aiChatApi.sendStreamMessage()` 请求 `/api/ai/agent/unified`
4. `SSEClient` 解析 `intent/message/plan/app_generated/done`
5. `ChatPanel` 汇总流式内容并渲染消息与预览卡片
6. `Preview.ets` 使用 HarmonyOS Web 组件加载预览页

### 前端网络设计

- `HttpManager` 单例化 `rcp.Session`，统一 header 和超时
- `AuthInterceptor` 为非白名单接口自动注入 JWT
- `ResponseInterceptor` 统一处理 `401/403` 并触发重新登录
- SSE 请求使用独立 session 管理生命周期
- `API_BASE_URL` 集中定义在 `utils/http/ApiConfig.ets`

## 环境要求

- JDK 21+
- PostgreSQL 14+
- Maven（或使用 `./mvnw`）
- DevEco Studio（运行 HarmonyOS 客户端）

## 环境变量

后端从 `apps/api/src/main/resources/application.yaml` 读取配置。

| 变量 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `SERVER_PORT` | 否 | `8080` | 后端端口 |
| `DB_DRIVER` | 否 | `org.postgresql.Driver` | JDBC 驱动 |
| `DB_URL` | 是 | - | PostgreSQL 连接串 |
| `DB_USERNAME` | 是 | - | 数据库用户名 |
| `DB_PASSWORD` | 是 | - | 数据库密码 |
| `JWT_SECRET` | 是 | - | JWT 签名密钥 |
| `JWT_EXPIRATION` | 否 | `8640000000` | Token 过期时间（毫秒） |
| `APP_STORAGE_PATH` | 否 | `data` | 生成应用存储根目录 |
| `API_KEY` | 是 | - | DashScope 模型密钥 |
| `MODEL_NAME` | 否 | `qwen-plus` | 聊天模型名 |
| `ZHIPU_API_KEY` | 是（logo） | - | 智谱图像模型密钥 |

## 快速开始

### 1. 启动后端

```bash
cd apps/api
./mvnw spring-boot:run
```

默认访问地址：`http://localhost:8080`

### 2. 查看接口文档

```text
http://localhost:8080/swagger-ui.html
```

### 3. 构建与测试

```bash
cd apps/api
./mvnw clean package
./mvnw test
```

运行单测类：

```bash
./mvnw test -Dtest=ClassName
```

### 4. 运行 HarmonyOS 客户端（可选）

```bash
cd apps/huawei
ohpm install
```

然后在 DevEco Studio 中运行 `entry` 模块。

## API 概览

`WebMvcConfig` 为控制器统一添加 `/api` 前缀。

### 公共接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/preview/{uuid}`
- `GET /api/preview/{uuid}/v/{versionNumber}`
- `GET /api/logo/{uuid}`

### 鉴权接口

- `GET /api/user`
- `PATCH /api/user`
- `POST /api/ai/agent/unified`
- `GET /api/ai/sessions`
- `POST /api/ai/sessions`
- `GET /api/ai/sessions/{sessionId}`
- `PATCH /api/ai/sessions/{sessionId}`
- `DELETE /api/ai/sessions/{sessionId}`
- `GET /api/ai/sessions/{sessionId}/messages`
- `POST /api/ai/messages`

## 数据与存储模型

### 会话消息

- `chat_sessions`: 会话信息
- `chat_messages`: 用户/助手消息，可携带 `related_app_id`、`related_version_id`

### 应用版本

- `apps`: 应用元数据与当前版本指针
- `app_versions`: 版本历史与存储路径
- 生成产物保存在文件系统（按应用与版本分目录）

## Prompt 与静态资源约定

代码生成 Prompt：

- `apps/api/src/main/resources/prompts/gen-code.txt`

当前约束：

- 使用本地 Tailwind
- 使用本地 Vue 3 运行时（`/public/js/vue.js`）
- 使用本地 Bootstrap Icons（`/public/css/bootstrap-icons.css` + fonts）

## 安全说明

- 受保护接口需要 JWT
- `StorageService` 需要持续保持路径归一化与边界校验
- 不要在源码中硬编码密钥
- 生产环境务必使用高强度 `JWT_SECRET`

## 开发建议

- 保持 `chat/gen/edit` pipeline 职责清晰
- 关键用户可见事件建议持久化，便于追踪与回放
- SSE 事件名和 payload 结构保持稳定
- 数据库变更一律通过 Flyway migration
- Prompt 改动建议审阅 diff 后再发布

## 常见问题

### 后端启动失败

- 检查 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- 确认 PostgreSQL 可达
- 检查 Flyway migration 日志

### 客户端收不到 SSE

- 检查 `Authorization` 是否为 `Bearer <token>`
- 检查 CORS 和设备网络连通性
- 确认响应类型为 `text/event-stream`

### 图标不显示

- 确认加载 `/public/css/bootstrap-icons.css`
- 确认 `public/css/fonts` 下字体文件存在
- 确认生成页引用的是本地资源路径

## 更多文档

- `docs/README.en.md`: 英文版 README
- `docs/backend.md`: 后端架构与演进记录
- `CLAUDE.md`: 仓库实现注意事项

## License

如需开源发布，请在仓库根目录补充 `LICENSE` 文件并更新本节。