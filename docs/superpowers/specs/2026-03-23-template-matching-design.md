# Template Matching for App Generation - Design

## Overview

When a user's intent is classified as `GEN` (generate application), the system will first check if an existing template matches the user's request. If a match is found, the template files will be copied directly to the new app version instead of calling OpenCode to generate code.

## Architecture

### Components

1. **TemplateMatcherService** - New service to scan templates and match user requests
2. **Template Match Prompt** - New prompt for AI-based template matching
3. **TemplateFileService** - New service to handle template file copying

### Flow

```
User Request → Intent Analysis (GEN)
    ↓
Parallel Execution (independent, no dependencies):
  ├─ TemplateMatcherService.matchTemplate(userMessage)
  │    → Returns matched template name or null
  │
  ├─ Logo Generation (existing)
  │    → Returns logo uuid
  │
  └─ Create App Entity
       → Returns app with ID
    ↓
After all parallel tasks complete:
  ├─ Template matched → Copy template files to AppVersion directory
  └─ No match → Use OpenCode to generate code (existing flow)
```

**Note:** Template matching only needs user message (no appId needed). File copy happens AFTER template matching result is available and AFTER app entity is created (to know destination path).

## Implementation Details

### 1. TemplateMatcherService

**Location:** `com.metacraft.api.modules.ai.service.TemplateMatcherService`

**Responsibilities:**
- Scan `data/templates/` directory on each request
- Build template list from folder names
- Use AI to match user message with templates
- Return matched template name or null

**LangChain4j Integration:**
```java
@AiService
public interface TemplateMatcherService {
    @SystemMessage(fromResource = "prompts/template-match.txt")
    @UserMessage("templates: {{templates}}\nuser request: {{message}}")
    Flux<String> matchTemplates(@V("templates") String templates, @V("message") String message);
}
```

**Key Methods:**
```java
public Mono<String> matchTemplate(String userMessage)
```

### 2. Template Match Prompt

**Location:** `src/main/resources/prompts/template-match.txt`

**Content:**
```
You are a template matching assistant. Given a user request and a list of available templates,
determine if any template matches the user's needs.

Available templates:
{{templates}}

User request: {{message}}

Respond with ONLY the template folder name (no quotes, no extra text).
If no template matches, respond with exactly: NONE
Trim any whitespace from your response.

Example:
- User wants a chess game → Respond: "象棋游戏_中国象棋对弈"
- User wants a weather app → Respond: "NONE"
```

### 3. AI Response Parsing

The AI response needs robust parsing to handle edge cases:
1. Trim whitespace and newlines
2. Check for exact "NONE" (case-insensitive)
3. Validate returned template name exists in the template directory
4. If validation fails, treat as no match

```java
private String parseTemplateMatchResponse(String response, List<String> templateNames) {
    String trimmed = response.trim();
    if (trimmed.equalsIgnoreCase("NONE")) {
        return null;
    }
    // Validate the returned name exists
    if (templateNames.contains(trimmed)) {
        return trimmed;
    }
    return null; // Invalid response, fallback to OpenCode
}

### 4. TemplateFileService

**Location:** `com.metacraft.api.modules.ai.service.TemplateFileService`

**Responsibilities:**
- Copy template files to app version directory
- Validate paths to prevent directory traversal
- Handle errors gracefully

**Security & Validation:**
1. Source path must be within template directory (prevent traversal)
2. Destination path must be within app storage directory
3. Reject dangerous file types: `.sh`, `.bat`, `.cmd`, `.exe`, `.ps1`
4. Only allow safe file types: `.html`, `.js`, `.css`, `.json`, `.vue`, `.png`, `.jpg`, `.jpeg`, `.svg`, `.gif`, `.woff2`

**File Type Validation:**
```java
private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
    ".html", ".js", ".css", ".json", ".vue", ".png", ".jpg", ".jpeg", ".svg", ".gif", ".woff2"
);
private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
    ".sh", ".bat", ".cmd", ".exe", ".ps1"
);
```

### 5. Template File Copy Logic

When a template is matched:
1. Scan the matched template directory for all files
2. Validate source path is within `data/templates/` (prevent traversal)
3. Copy each file to `apps/{appId}/v{version}/` directory
4. Validate destination path is within app storage directory
5. Continue with existing AppVersion creation flow

### 6. Error Handling

| Scenario | Handling |
|----------|----------|
| Template directory not found | Fallback to OpenCode |
| Template copy fails | Fallback to OpenCode |
| No template matches (returns "NONE") | Fallback to OpenCode |
| Template matching timeout | Fallback to OpenCode |
| OpenCode also fails | Return error event |

## Template Directory Structure

The template directory should be located within the configurable storage path.

```
{app.storage.path}/templates/
├── 围棋游戏_一个在线围棋对弈游戏/
│   ├── index.html
│   └── app.js
├── 象棋游戏_中国象棋对弈/
│   ├── index.html
│   └── app.js
└── {new template}/
    ├── index.html
    ├── app.js
    └── {other files}.{ext}
```

**Naming Convention:** `{project_name}_{description}` (single underscore, matching existing templates)

## Integration with AppGenPipelineService

### Modified Flow

1. Intent is determined to be GEN
2. Execute in parallel:
   - Template matching (new)
   - Logo generation (existing)
   - App entity creation (existing)
3. After template matching result:
   - If match found (not "NONE"): Copy template files instead of calling OpenCode
   - If no match: Use existing OpenCode flow
4. Generate SSE events: `intent` → `app_info` → `logo_generated` → `app_generated` → `done`

### SSE Events

| Event | Description |
|-------|-------------|
| `intent` | Intent classification result |
| `app_info` | App name and description |
| `logo_generated` | Logo generated |
| `app_generated` | App generation complete (with url, uuid) |
| `error` | Error occurred (e.g., both template and OpenCode failed) |
| `done` | Stream ended |

## Configuration

- **Config key:** `app.templates.path` (default: `data/templates/`)
- **Timeout:** `app.templates.timeout-millis` (default: 10000ms)
- Template scan: Dynamic on each request (no caching)

**application.yaml example:**
```yaml
app:
  templates:
    path: data/templates/
    timeout-millis: 10000
```

## Testing Considerations

1. Test with matching template
2. Test with no matching template (fallback to OpenCode)
3. Test with empty template directory
4. Test with template directory not existing
5. Test with multiple potential matches (AI should pick best)
6. Test path traversal prevention (ensure malicious paths are rejected)
7. Test template copy failure handling

## Model Selection

**Recommendation:** Use a fast/light model (e.g., qwen-turbo) for template matching to minimize latency, as this is a simple classification task.

## Timeout Configuration

Template matching should have a timeout (e.g., 10 seconds) to prevent hanging. If timeout occurs, fallback to OpenCode.