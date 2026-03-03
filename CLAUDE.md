# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MetaCraft is a full-stack AI-driven application generation platform. Users can create web applications through natural language conversation. The system uses LangChain4j for AI integration with intent recognition to automatically determine whether users want to chat or generate applications.

**Core Philosophy**: The AI assistant ("元梦/YuanMeng") can automatically save generated applications via `@Tool`-annotated methods - users simply describe what they want, and the AI handles the entire flow including code generation and persistence.

## Build Commands

### Backend (Spring Boot API)
```bash
cd apps/api
./mvnw spring-boot:run          # Start development server (port 8080)
./mvnw clean package            # Build for production
./mvnw test                     # Run tests
```

Access Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Frontend (HarmonyOS App)
Uses DevEco Studio for development. Build config in `hvigor/hvigor-config.json5`.

```bash
cd apps/huawei
ohpm install                    # Install dependencies
```

### Running Tests
```bash
cd apps/api
./mvnw test                     # Run all tests
./mvnw test -Dtest=ClassName    # Run specific test class
./mvnw test -Dtest=ClassName#methodName  # Run specific test method
```

## Technology Stack

**Backend (apps/api/):**
- Java 21, Spring Boot 3.5.9
- LangChain4j 1.11.0-beta19 (DashScope/Qwen models via `langchain4j-community-dashscope-spring-boot-starter`)
- Spring Security with JWT (java-jwt 4.4.0)
- Spring Data JPA, PostgreSQL
- Flyway for database migrations
- Reactor (`Flux<String>`) for SSE streaming
- SpringDoc OpenAPI 2.8.14 for Swagger
- Lombok for reducing boilerplate (configured in maven-compiler-plugin)

**Frontend (apps/huawei/):**
- ArkTS (TypeScript variant for HarmonyOS)
- ETS (Extensible Type Script) for UI
- OHPM for package management
- @luvi/lv-markdown-in for markdown rendering
- Custom SSE client using `@kit.RemoteCommunicationKit` (rcp)

### SSE Event Types

The unified endpoint emits custom event types:

| Event | Description | Frontend Handler |
|-------|-------------|------------------|
| `intent` | Intent classification result ("chat" or "gen") | `onIntent()` |
| `message` | Streaming AI response content | `onMessage()` |
| `plan` | App generation plan (gen mode only) | `onPlan()` |
| `app_generated` | App generation complete with preview URL | `onAppGenerated()` |
| `done` | Stream ended | `onDone()` |

## Architecture

### Unified Agent Flow

The single endpoint `POST /api/ai/agent/unified` handles everything via SSE:

```
User message → AgentService.unified()
    ↓
AgentIntentService.classifyIntent()  (returns "chat" or "gen")
    ↓
    ├─ "chat" → AgentAiService.chat() → conversation
    └─ "gen" → AgentAiService.generateApp() → generates HTML, calls @Tool saveApp()
    ↓
SSE response: event:intent → event:message (streaming)
```

### LangChain4j Integration (Critical)

**@AiService Pattern:**
```java
@AiService
public interface AgentAiService {
    @SystemMessage(fromResource = "prompts/agent-chat.txt")
    Flux<String> chat(@UserMessage String message);

    @SystemMessage(fromResource = "prompts/agent-gen-app.txt")
    Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
}
```
- LangChain4j auto-generates implementation at startup
- `Flux<String>` enables reactive streaming
- `@V("varName")` injects variables into prompt templates (use `{{varName}}` in template)

**@Tool Pattern:**
```java
@Tool("Save the generated application code, name, and description. Returns the preview URL.")
public String saveApp(
    @P("The name of the application") String name,
    @P("A short description of the application") String description,
    @P("The complete HTML source code") String code,
    @P("The user ID provided in the context") Long userId
) { ... }
```
- AI automatically calls this after generating app code
- `@P` parameter descriptions tell AI how to use the tool
- Tool must return a string (typically URL or status)

**Prompt Templates** (`src/main/resources/prompts/`):
- `agent-system.txt` - Generic system prompt
- `agent-chat.txt` - Chat mode system prompt
- `agent-gen-app.txt` - App generation prompt (instructs AI to use Tailwind CSS, Alpine.js, call saveApp)
- `intent-classification.txt` - Intent recognition prompt
- `image-logo-gen.txt` - Logo generation prompt

### Frontend API Layer

HarmonyOS app uses a layered API architecture:

```
UI Components (pages/)
    ↓
API Modules (api/)
    ↓
HttpManager (utils/http/)
    ↓
SSEClient / rcp.Session
```

**Key Patterns:**
- API modules (e.g., `aiChat.ets`) define request/response interfaces
- `HttpManager` wraps `rcp.Session` with interceptors
- `AuthInterceptor` automatically injects JWT tokens
- `SSEClient` handles streaming responses with custom event parsing

### Application Version Management

App creation flow:
1. `AppService.createApp()` creates app entry with UUID
2. `AppService.createVersion()` saves HTML to `apps/{appId}/v{version}/index.html`
3. Database tracks metadata; filesystem stores code
4. Preview URL: `/api/preview/{uuid}` (latest) or `/api/preview/{uuid}/v/{versionNumber}`

## Environment Variables

```
DB_URL=jdbc:postgresql://localhost:5432/metacraft
DB_USERNAME=your_username
DB_PASSWORD=your_password
API_KEY=your_dashscope_api_key
MODEL_NAME=qwen-plus
JWT_SECRET=change-this-secret-key-in-production (use 256+ bits in production)
JWT_EXPIRATION=8640000000
APP_STORAGE_PATH=apps/data
SERVER_PORT=8080
```

## Database Schema

Flyway migrations in `apps/api/src/main/resources/db/migration/`:

| Table | Purpose |
|-------|---------|
| `users` | JWT auth, email, password_hash (BCrypt), avatar_base64, bio |
| `apps` | App metadata (uuid, user_id, name, description, current_version_id) |
| `app_versions` | Version history (app_id, version_number, storage_path, change_log) |
| `chat_sessions` | Sessions (uuid, user_id, app_id, title) |
| `chat_messages` | Messages (session_id, role: user/assistant, content) |

## Key API Endpoints

**Public:**
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - Login (returns JWT token)
- `GET /api/preview/{uuid}` - Preview app (latest version)
- `GET /api/preview/{uuid}/v/{version}` - Preview specific version

**Authenticated (Authorization: Bearer {token}):**
- `GET /api/user` - Get current user
- `PATCH /api/user` - Update user
- `POST /api/ai/agent/unified` - Main AI SSE endpoint
- `GET /api/ai/sessions` - List chat sessions
- `POST /api/ai/sessions` - Create session
- `GET /api/ai/sessions/{id}/messages` - Get session messages

**Note**: `/api/ai/agent/**` uses manual validation in controller, not Spring Security.

## Project Structure

```
apps/
├── api/                    # Spring Boot backend
│   └── src/main/
│       ├── java/com/metacraft/api/
│       │   ├── config/         # SecurityConfig, WebMvcConfig, OpenApiConfig
│       │   ├── security/       # JWT: JwtTokenProvider, JwtAuthenticationFilter
│       │   ├── exception/      # GlobalExceptionHandler
│       │   ├── response/       # ApiResponse, Response builders
│       │   └── modules/
│       │       ├── ai/         # CORE: controllers, @AiService, @Tool, entities
│       │       ├── user/       # AuthController, UserController, UserService
│       │       ├── app/        # PreviewController, AppService, entities
│       │       └── storage/    # StorageService (file I/O with path validation)
│       └── resources/
│           ├── application.yaml
│           ├── db/migration/   # V1__*.sql to V5__*.sql
│           └── prompts/        # AI prompt templates
└── huawei/                # HarmonyOS app
    ├── entry/src/main/ets/
    │   ├── pages/         # Page components (Index, Explore, Profile, Preview, etc.)
    │   ├── components/    # Reusable components (ChatPanel, SessionList, AppPreviewCard)
    │   ├── api/           # API client modules (auth.ets, aiChat.ets, chatSession.ets)
    │   ├── utils/http/    # HTTP utilities (HttpManager, SSEClient, Interceptors)
    │   └── model/         # Data models (auth.ets, aiChatModel.ets, chatSessionModel.ets)
    └── ohos_agcit/        # Common modules (login, collect info, settings)
```

## Security Notes

- JWT tokens contain user ID and email
- BCrypt for password hashing
- `StorageService` validates paths with `normalize()` and `startsWith()` to prevent directory traversal
- UUIDs used for public app access (prevents ID enumeration)
- Preview endpoints are publicly accessible (no auth required)

## Common Tasks

**Modifying AI behavior:**
- Edit prompt templates in `src/main/resources/prompts/`
- Adjust `AgentIntentService` for intent classification logic
- Add new `@Tool` methods in `AgentToolService`

**Adding a new API endpoint:**
1. Create DTO in `modules/*/dto/`
2. Add controller method with appropriate mapping
3. Implement service logic
4. Update `SecurityConfig` if endpoint needs auth

**Database changes:**
1. Create new Flyway migration `V{n}__description.sql`
2. Run `./mvnw spring-boot:run` - Flyway auto-applies migrations

**Frontend API integration:**
1. Define request/response interfaces in `apps/huawei/entry/src/main/ets/api/`
2. Add endpoint methods using `HttpManager` or `SSEClient`
3. Update `Interceptors.ets` if authentication headers are needed
4. Handle SSE events in components using `SSEListener` interface

## Important Gotchas

1. **LangChain4j tools must be Spring beans** - Use `@Service` or `@Component`
2. **@Tool methods should return String** - The AI receives the return value
3. **SSE streaming requires Flux<ServerSentEvent<String>>** - Map Flux<String> to SSE events in controller
4. **File paths use app ID, not UUID** - UUID is for external access, internal storage uses numeric ID
5. **AI prompt templates use {{variable}} syntax** - Must match `@V("variable")` parameter names
6. **Manual JWT validation in agent endpoints** - `/api/ai/agent/**` uses `AuthUtils.validateAuthorization()` instead of Spring Security
7. **RCP error code 1007900992** - In HarmonyOS SSE client, this means "Request canceled" and is expected when closing sessions
8. **SSE text decoder must use stream mode** - `decoder.decodeToString(buffer, { stream: true })` handles multi-byte UTF-8 characters across chunks
9. **StorageService path validation** - Always use `normalize()` and `startsWith()` to prevent directory traversal attacks
10. **Flyway migrations run automatically** - New migrations in `db/migration/` apply on server startup
