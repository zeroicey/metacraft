# MetaCraft

MetaCraft is an AI-driven application generation platform. Users describe what they want in natural language, and the system can chat, generate app plans, produce runnable web code, persist versions, and stream progress back to clients in real time.

中文文档: `../README.md`

## Repository Overview

This repository contains:
- `apps/api`: Spring Boot backend (core AI orchestration, auth, app/version persistence, SSE streaming)
- `apps/huawei`: HarmonyOS client
- `docs`: supplemental architecture docs

## Why MetaCraft

- Unified AI entrypoint with intent routing (`chat`, `gen`, `edit`)
- Real-time SSE event streaming for responsive UX
- Built-in session and message persistence
- App/version lifecycle management with filesystem-backed artifacts
- Extensible AI capability design (`IntentAnalyzer`, pipeline services, tool-like domain services)

## System Architecture

At runtime, requests flow through a unified endpoint and are routed by intent:

1. Client sends `POST /api/ai/agent/unified`
2. Backend validates auth token and user identity
3. `UnifiedOrchestrator` resolves/creates session and stores the user message
4. Intent is analyzed and routed to pipeline:
   - `ChatPipelineService`
   - `AppGenPipelineService`
   - `AppEditPipelineService`
5. Pipeline streams SSE events and persists assistant outputs

### Current SSE Event Types

| Event | Meaning |
| --- | --- |
| `intent` | Classified intent for the request |
| `message` | Streaming AI response content |
| `plan` | App generation plan stream |
| `app_info` | Extracted app metadata (name/description) |
| `logo_generated` | Logo generation finished |
| `app_generated` | App code persisted and preview available |
| `error` | Error event with message payload |

## Repository Layout

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

## Tech Stack

### Backend (`apps/api`)

- Java 21
- Spring Boot 3.5.x
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Flyway
- Reactor (`Flux`) + SSE
- LangChain4j
- Swagger/OpenAPI (`springdoc`)

### Frontend (`apps/huawei`)

- HarmonyOS (ArkTS/ETS)
- OHPM package management
- Custom SSE client implementation
- AGCIT common capability modules (`aggregated_login`, `collect_personal_info`, `app_setting`)

## HarmonyOS Frontend Guide

The HarmonyOS client is under `apps/huawei`, built with ArkTS and organized in layered modules.

### Frontend Structure

- `apps/huawei/AppScope`: app-level metadata (bundle name, icon, app label)
- `apps/huawei/entry`: main module and page entry
- `apps/huawei/entry/src/main/ets/pages`: page containers (`Index`, `Explore`, `Preview`, `Profile`, `Setting`, etc.)
- `apps/huawei/entry/src/main/ets/components`: reusable UI components (`ChatPanel`, `AppPreviewCard`, sidebar/menu components)
- `apps/huawei/entry/src/main/ets/api`: API wrappers (`auth`, `aiChat`, `chatSession`, `appApi`)
- `apps/huawei/entry/src/main/ets/utils/http`: HTTP/SSE infrastructure (`HttpManager`, interceptors, `SSEClient`, API config)
- `apps/huawei/entry/src/main/ets/model`: typed request/response and model utilities
- `apps/huawei/ohos_agcit`: shared login/privacy/settings modules

### Frontend Runtime Flow

1. `Index.ets` mounts `ChatPanel`.
2. Login state is checked on appear (`authApi.isLoggedIn()`), with global unauthorized callback wiring.
3. Chat requests go through `aiChatApi.sendStreamMessage()` to `/api/ai/agent/unified`.
4. `SSEClient` parses stream chunks (`intent`, `message`, `plan`, `app_generated`, `done`).
5. `ChatPanel` merges chunks into assistant messages and app preview cards.
6. Preview navigation opens `Preview.ets`, rendering generated apps in HarmonyOS Web component.

### Frontend Networking Design

- `HttpManager` creates a singleton `rcp.Session` with unified headers/timeouts.
- `AuthInterceptor` injects JWT for non-whitelisted routes.
- `ResponseInterceptor` handles `401/403`, clears invalid token, and triggers login sheet callback.
- SSE stream requests use independent `rcp.Session` instances in `SSEClient` for lifecycle control.
- Base URL is centralized in `apps/huawei/entry/src/main/ets/utils/http/ApiConfig.ets`.

## Prerequisites

Install the following before running locally:

- JDK 21+
- PostgreSQL 14+
- Maven (or use `./mvnw` wrapper)
- HarmonyOS toolchain (DevEco Studio)

## Environment Variables

The backend reads configuration from environment variables (see `apps/api/src/main/resources/application.yaml`).

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `SERVER_PORT` | No | `8080` | Backend port |
| `DB_DRIVER` | No | `org.postgresql.Driver` | JDBC driver |
| `DB_URL` | Yes | - | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | - | DB username |
| `DB_PASSWORD` | Yes | - | DB password |
| `JWT_SECRET` | Yes | - | JWT signing secret |
| `JWT_EXPIRATION` | No | `8640000000` | Token expiration (ms) |
| `APP_STORAGE_PATH` | No | `data` | App artifact storage root |
| `API_KEY` | Yes | - | DashScope model key |
| `MODEL_NAME` | No | `qwen-plus` | Chat model name |
| `ZHIPU_API_KEY` | Yes (for logo) | - | Zhipu image model key |

## Quick Start

### 1) Start Backend

```bash
cd apps/api
./mvnw spring-boot:run
```

### 2) API Documentation

```text
http://localhost:8080/swagger-ui.html
```

### 3) Build and Test

```bash
cd apps/api
./mvnw clean package
./mvnw test
```

Run a specific test class:

```bash
./mvnw test -Dtest=ClassName
```

### 4) Run HarmonyOS Client (Optional)

```bash
cd apps/huawei
ohpm install
```

Then run from DevEco Studio.

## API Overview

All `@RestController` routes are prefixed with `/api` by `WebMvcConfig`.

### Public Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/preview/{uuid}`
- `GET /api/preview/{uuid}/v/{versionNumber}`
- `GET /api/logo/{uuid}`

### Authenticated Endpoints

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

## Prompt and Static Asset Conventions

Code generation prompt:

- `apps/api/src/main/resources/prompts/gen-code.txt`

Current constraints:

- Local Tailwind usage
- Native JavaScript only (no jQuery)
- Local Bootstrap Icons usage via:
  - `/public/css/bootstrap-icons.css`
  - `/public/css/fonts/bootstrap-icons.woff2`
  - `/public/css/fonts/bootstrap-icons.woff`

## Additional Documentation

- `../README.md`: Chinese default README
- `backend.md`: backend architecture notes
- `../CLAUDE.md`: repository implementation notes and gotchas

## License

If you plan to open-source or distribute this project, add a root `LICENSE` file and update this section accordingly.
