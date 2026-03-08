# MetaCraft

MetaCraft is an AI-driven application generation platform. Users describe what they want in natural language, and the system can chat, generate app plans, produce runnable web code, persist versions, and stream progress back to clients in real time.

This repository contains:
- `apps/api`: Spring Boot backend (core AI orchestration, auth, app/version persistence, SSE streaming)
- `apps/huawei`: HarmonyOS client
- `docs`: architecture and backend design notes

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

The backend currently emits these events:

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
|  |- api/                  # Spring Boot backend
|  |  |- src/main/java/com/metacraft/api/
|  |  |  |- config/
|  |  |  |- modules/
|  |  |     |- ai/          # Unified orchestrator, pipelines, chat/session/message modules
|  |  |     |- app/         # App metadata/versioning/preview/logo
|  |  |     |- user/        # Auth + user profile
|  |  |- src/main/resources/
|  |     |- db/migration/   # Flyway SQL migrations
|  |     |- prompts/        # AI prompt templates
|  |     |- public/         # Static assets served at /public/**
|  |- huawei/               # HarmonyOS client project
|- docs/                    # Additional architecture docs
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

## Prerequisites

Install the following before running locally:

- JDK 21+
- PostgreSQL 14+
- Maven (or use `./mvnw` wrapper)
- HarmonyOS toolchain (DevEco Studio) if you run the client

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

Backend will start on `http://localhost:8080` (unless overridden).

### 2) API Documentation

Open Swagger UI:

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
- `POST /api/ai/agent/unified` (manual token validation for this route family)
- `GET /api/ai/sessions`
- `POST /api/ai/sessions`
- `GET /api/ai/sessions/{sessionId}`
- `PATCH /api/ai/sessions/{sessionId}`
- `DELETE /api/ai/sessions/{sessionId}`
- `GET /api/ai/sessions/{sessionId}/messages`
- `POST /api/ai/messages`

## Persistence Model

### Chat

- `chat_sessions`: conversation sessions
- `chat_messages`: user/assistant messages, plus optional `related_app_id` and `related_version_id`

### App Generation

- `apps`: app metadata + current version pointer
- `app_versions`: version history and storage path
- Artifacts are written to filesystem under app-specific directories

## Prompt and Static Asset Conventions

Code generation prompt is located at:

- `apps/api/src/main/resources/prompts/gen-code.txt`

Current constraints include:

- Local Tailwind usage
- Local jQuery usage (version `4.0.0`)
- Local Bootstrap Icons usage (version `1.13.1`) via:
	- `/public/css/bootstrap-icons.css`
	- `/public/css/fonts/bootstrap-icons.woff2`
	- `/public/css/fonts/bootstrap-icons.woff`

## Security Notes

- JWT is required for protected routes
- `StorageService` path checks should remain normalized and bounded
- Do not hardcode secrets in source
- Use strong JWT secret in production

## Development Guidelines

- Keep pipeline responsibilities separated (chat/gen/edit)
- Persist critical user-visible events/messages for traceability
- Prefer explicit SSE event names and stable payload structures
- Add Flyway migration for every schema change
- Keep prompts under version control and review prompt diffs carefully

## Troubleshooting

### Backend fails at startup

- Verify DB credentials (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- Ensure PostgreSQL is reachable
- Check Flyway migration errors in logs

### SSE stream not received by client

- Verify `Authorization` header format: `Bearer <token>`
- Check CORS behavior for your client environment
- Inspect network response as `text/event-stream`

### Logo generated but icon not displayed

- Ensure `/public/css/bootstrap-icons.css` is loaded
- Ensure font files exist under `/public/css/fonts/`
- Confirm generated HTML references local asset paths

## Additional Documentation

- `docs/backend.md`: target backend architecture and evolution notes
- `CLAUDE.md`: repository-level implementation notes and gotchas

## License

If you plan to open-source or distribute this project, add a root `LICENSE` file and update this section accordingly.