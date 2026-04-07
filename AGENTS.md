# AGENTS.md - MetaCraft Development Guide

This file provides guidance for AI agents operating in this repository.

## Project Overview

MetaCraft is a full-stack AI-driven application generation platform. Users can create web applications through natural language conversation. The system uses LangChain4j for AI integration.

**Backend**: Spring Boot 3.5.9 (Java 21)  
**Database**: PostgreSQL with Flyway migrations  
**Frontend**: HarmonyOS (ArkTS)

---

## Build & Run Commands

### Backend (Spring Boot API)

```bash
# Start development server (port 8080)
cd apps/api && ./mvnw spring-boot:run

# Build for production
cd apps/api && ./mvnw clean package

# Run all tests
cd apps/api && ./mvnw test

# Run specific test class
cd apps/api && ./mvnw test -Dtest=ClassName

# Run specific test method
cd apps/api && ./mvnw test -Dtest=ClassName#methodName

# Run tests with coverage (if configured)
cd apps/api && ./mvnw test -Dcoverage
```

### Frontend (HarmonyOS)

```bash
# Install dependencies
cd apps/huawei && ohpm install
```

### Environment Variables

Required variables (set in environment or `.env`):
```bash
DB_URL=jdbc:postgresql://localhost:5432/metacraft
DB_USERNAME=your_username
DB_PASSWORD=your_password
API_KEY=your_dashscope_api_key
MODEL_NAME=qwen-plus
JWT_SECRET=your-256-bit-secret-key
APP_STORAGE_PATH=apps/data
SERVER_PORT=8080
```

---

## Code Style Guidelines

### Package Structure

```
com.metacraft.api/
├── config/           # Configuration classes
├── security/         # JWT, auth utilities
├── exception/        # GlobalExceptionHandler
├── response/         # ApiResponse, Response builders
└── modules/
    └── {module}/
        ├── controller/
        ├── service/
        ├── entity/
        ├── dto/
        ├── vo/
        ├── repository/
        ├── converter/
        └── agent/
```

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Controller | `{Name}Controller.java` | `UserController.java` |
| Service | `{Name}Service.java` | `AppService.java` |
| Entity | `{Name}Entity.java` | `AppEntity.java` |
| DTO | `{Entity}{Operation}DTO.java` | `AppCreateDTO.java` |
| VO | `{Name}VO.java` | `AppVO.java` |
| Repository | `{Entity}Repository.java` | `AppRepository.java` |
| Converter | `{Entity}Converter.java` | `UserConverter.java` |

### Import Ordering

1. `java.*` packages
2. `org.springframework.*`
3. `com.metacraft.*` (internal)
4. `lombok.*`
5. `jakarta.*`
6. Other external libraries

### Lombok Usage

Use Lombok to reduce boilerplate:
```java
@Service
@RequiredArgsConstructor
public class AppService {
    private final AppApplicationService appApplicationService;
    private final AppVersionService appVersionService;
}
```

Common annotations:
- `@Service`, `@Repository`, `@Component`
- `@RestController`, `@ControllerAdvice`
- `@Getter`, `@Setter`
- `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- `@RequiredArgsConstructor` (for constructor injection)

### Entity Patterns

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
    private String uuid;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

### DTO & Validation

Use Jakarta Validation for request validation:
```java
@Getter
@Setter
public class UserRegisterDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotNull(message = "简介不能为空")
    private String bio;
}
```

### Response Patterns

Use `ApiResponse<T>` record for consistent API responses:
```java
public record ApiResponse<T>(String message, T data, Object error) {
    public static <T> ApiResponse<T> ok(String message, T data) { ... }
    public static <T> ApiResponse<T> fail(String message) { ... }
}

// In controller:
return ApiResponse.ok("Success", data);
```

Or use `Response` builder:
```java
return Response.error("Error message").status(HttpStatus.BAD_REQUEST.value()).build();
```

### Controller Patterns

```java
@RestController
@RequestMapping("/api/apps")
@Tag(name = "应用管理", description = "应用相关接口")
public class AppController {

    private final AppService appService;

    public AppController(AppService appService) {
        this.appService = appService;
    }

    @PostMapping
    @Operation(summary = "创建应用")
    public ResponseEntity<ApiResponse<AppVO>> createApp(
            @Valid @RequestBody AppCreateDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        // ...
    }
}
```

### Error Handling

All exceptions are handled by `GlobalExceptionHandler`:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(...) { ... }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(...) { ... }
}
```

Custom exceptions should extend `RuntimeException` or use `ResponseStatusException`.

### SSE (Server-Sent Events) Streaming

Use `Flux<ServerSentEvent<String>>` for streaming responses:
```java
@PostMapping(value = "/unified")
public Flux<ServerSentEvent<String>> unified(
        @Valid @RequestBody AgentRequestDTO request,
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return unifiedOrchestrator.handleRequest(request, userId)
        .map(content -> ServerSentEvent.builder(data).build());
}
```

### JWT Authentication

Manual JWT validation in controllers:
```java
@PostMapping("/unified")
public Flux<ServerSentEvent<String>> unified(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
    AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
    String token = authHeader.substring(7);
    Long userId = jwtTokenProvider.getUserIdFromToken(token);
    // ...
}
```

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`:
- Naming: `V{n}__{description}.sql`
- Migrations run automatically on startup
- Use `ddl-auto: validate` in application.yaml

### File Storage

Use `StorageService` for file operations with path validation (prevents directory traversal):
```java
private final StorageService storageService;

// Paths are validated with normalize() and startsWith()
String path = storageService.saveFile(appId, filename, content);
```

---

## Architecture Patterns

### Service Layer Organization

- **Query Services**: `AppQueryService` - read operations
- **Application Services**: `AppApplicationService` - write operations
- **Version Services**: `AppVersionService` - version management

### AI Integration

Use LangChain4j `@AiService` with `@Tool` annotations:
```java
@AiService
public interface AgentAiService {
    @SystemMessage(fromResource = "prompts/agent-gen-app.txt")
    Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
}

@Tool("Save the generated application")
public String saveApp(
    @P("The name of the application") String name,
    @P("The complete HTML source code") String code,
    @P("The user ID provided in the context") Long userId
) { ... }
```

### SSE Event Types

| Event | Description |
|-------|-------------|
| `intent` | Intent classification result (JSON: "chat" or "gen") |
| `message` | Streaming AI response content |
| `plan` | App generation plan |
| `app_generated` | App generation complete (JSON: {url, uuid, name}) |
| `done` | Stream ended |

---

## Testing Guidelines

Tests should be placed in `src/test/java/` mirroring the main source structure. Use:
- JUnit 5 for unit tests
- MockMvc for controller tests
- Mockito for mocking dependencies
- Testcontainers for integration tests (if needed)

---

## Swagger Documentation

API documentation available at `http://localhost:8080/swagger-ui.html`.

Use annotations:
```java
@Operation(summary = "创建应用", description = "创建一个新应用")
@Tag(name = "应用管理", description = "应用相关接口")
```

---

## Common Patterns

### Chain of Operations

```
Controller → Service (orchestration) → ApplicationService (write) + QueryService (read) → Repository
```

### ID vs UUID

- **Internal**: Use numeric `id` for database operations
- **External**: Use `uuid` (String) for public-facing URLs (`/api/preview/{uuid}`)

### Security Notes

- Validate all user inputs
- Use path normalization to prevent directory traversal
- Never expose secrets in responses
- Validate JWT on every protected endpoint