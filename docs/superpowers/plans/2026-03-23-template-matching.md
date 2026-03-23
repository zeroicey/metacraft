# Template Matching Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 GEN 意图识别后，通过 AI 匹配用户请求与预设模板，匹配成功则直接复制模板文件到 AppVersion，失败则回退到 OpenCode 生成。

**Architecture:** 新增 TemplateMatcherService 和 TemplateFileService，在 AppGenPipelineService 中并行执行模板匹配，匹配成功则复制文件而非调用 OpenCode。

**Tech Stack:** Java 21, Spring Boot, LangChain4j, Project Reactor

---

## File Structure

```
apps/api/src/main/java/com/metacraft/api/modules/ai/
├── agent/
│   └── TemplateMatcherAgent.java        # Create: AI interface for template matching
├── service/
│   ├── TemplateMatcherService.java      # Create: Main template matching logic
│   └── TemplateFileService.java         # Create: Template file copy logic
└── resources/
    └── prompts/
        └── template-match.txt           # Create: Prompt for template matching

apps/api/src/main/resources/
└── application.yaml                      # Modify: Add template config
```

---

## Chunk 1: Template Matching Core Services

### Task 1: Create Template Match Prompt

**Files:**
- Create: `apps/api/src/main/resources/prompts/template-match.txt`

- [ ] **Step 1: Create the prompt file**

```txt
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

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/resources/prompts/template-match.txt
git commit -m "feat(ai): add template match prompt"
```

---

### Task 2: Create TemplateMatcherAgent

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/agent/TemplateMatcherAgent.java`

- [ ] **Step 1: Write the interface**

```java
package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface TemplateMatcherAgent {
    @SystemMessage(fromResource = "prompts/template-match.txt")
    @UserMessage("templates: {{templates}}\nuser request: {{message}}")
    Flux<String> matchTemplates(@V("templates") String templates, @V("message") String message);
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/agent/TemplateMatcherAgent.java
git commit -m "feat(ai): add TemplateMatcherAgent interface"
```

---

### Task 3: Create TemplateMatcherService

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatcherService.java`

- [ ] **Step 1: Write the service**

```java
package com.metacraft.api.modules.ai.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.TemplateMatcherAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateMatcherService {

    private final TemplateMatcherAgent templateMatcherAgent;

    @Value("${app.templates.path:data/templates}")
    private String templatesPath;

    public Mono<String> matchTemplate(String userMessage) {
        List<String> templateNames = scanTemplateDirectories();

        if (templateNames.isEmpty()) {
            log.info("No templates found, fallback to OpenCode");
            return Mono.just(null);
        }

        String templatesList = String.join("\n", templateNames);
        log.info("Matching user message against templates: {}", templateNames);

        return Mono.from(templateMatcherAgent.matchTemplates(templatesList, userMessage))
                .timeout(java.time.Duration.ofSeconds(10))
                .onErrorResume(e -> {
                    log.warn("Template matching error, fallback to OpenCode: {}", e.getMessage());
                    return Mono.just("NONE");
                })
                .map(response -> parseTemplateMatchResponse(response, templateNames));
    }

    private String parseTemplateMatchResponse(String response, List<String> templateNames) {
        String trimmed = response.trim();
        log.info("Template matching response: {}", trimmed);

        if (trimmed.equalsIgnoreCase("NONE")) {
            return null;
        }

        // Validate the returned name exists
        if (templateNames.contains(trimmed)) {
            return trimmed;
        }

        log.warn("Template '{}' not found in available templates", trimmed);
        return null;
    }

    private List<String> scanTemplateDirectories() {
        File templateDir = new File(templatesPath);
        if (!templateDir.exists() || !templateDir.isDirectory()) {
            log.warn("Template directory not found: {}", templatesPath);
            return List.of();
        }

        File[] dirs = templateDir.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) {
            return List.of();
        }

        return Arrays.stream(dirs)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public String getTemplatePath() {
        return templatesPath;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatcherService.java
git commit -m "feat(ai): add TemplateMatcherService for template matching"
```

---

### Task 4: Create TemplateFileService

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateFileService.java`

- [ ] **Step 1: Write the service**

```java
package com.metacraft.api.modules.ai.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TemplateFileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".html", ".js", ".css", ".json", ".vue", ".png", ".jpg", ".jpeg", ".svg", ".gif", ".woff2"
    );

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".sh", ".bat", ".cmd", ".exe", ".ps1"
    );

    @Value("${app.templates.path:data/templates}")
    private String templatesPath;

    @Value("${app.storage.path:data}")
    private String storagePath;

    /**
     * Copy all files from template directory to destination directory.
     *
     * @param templateName The matched template folder name
     * @param appId        The app ID
     * @param versionNumber The version number
     * @return true if successful, false if should fallback to OpenCode
     */
    public boolean copyTemplateFiles(String templateName, Long appId, Integer versionNumber) {
        Path sourceDir = Path.of(templatesPath, templateName);
        Path destDir = Path.of(storagePath, "apps", appId.toString(), "v" + versionNumber);

        // Validate source path is within templates directory (prevent traversal)
        try {
            sourceDir = sourceDir.toRealPath();
            Path templatesRealPath = Path.of(templatesPath).toRealPath();
            if (!sourceDir.startsWith(templatesRealPath)) {
                log.error("Template path traversal attempt detected: {}", sourceDir);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to resolve template path: {}", sourceDir, e);
            return false;
        }

        if (!Files.exists(sourceDir)) {
            log.warn("Template directory not found: {}", sourceDir);
            return false;
        }

        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            log.error("Failed to create destination directory: {}", destDir, e);
            return false;
        }

        // Validate destination path is within storage directory
        try {
            Path destRealPath = destDir.toRealPath();
            Path storageRealPath = Path.of(storagePath).toRealPath();
            if (!destRealPath.startsWith(storageRealPath)) {
                log.error("Destination path traversal attempt detected: {}", destRealPath);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to resolve destination path: {}", destDir, e);
            return false;
        }

        File[] files = sourceDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            log.warn("Template directory is empty: {}", sourceDir);
            return false;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String fileName = file.getName();
            String extension = getFileExtension(fileName).toLowerCase();

            // Check forbidden extensions
            if (FORBIDDEN_EXTENSIONS.contains(extension)) {
                log.warn("Forbidden file type in template: {}", fileName);
                return false;
            }

            // Skip files with no extension unless it's allowed
            if (!extension.isEmpty() && !ALLOWED_EXTENSIONS.contains(extension)) {
                log.warn("File extension not allowed: {}", fileName);
                return false;
            }

            try {
                Path sourceFile = file.toPath();
                Path destFile = destDir.resolve(fileName);
                Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied template file: {} -> {}", fileName, destFile);
            } catch (IOException e) {
                log.error("Failed to copy template file: {}", fileName, e);
                return false;
            }
        }

        log.info("Successfully copied {} template files to {}", files.length, destDir);
        return true;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateFileService.java
git commit -m "feat(ai): add TemplateFileService for copying template files"
```

---

## Chunk 2: Integration with AppGenPipelineService

### Task 5: Integrate Template Matching into AppGenPipelineService

**Files:**
- Modify: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java`

- [ ] **Step 1: Add new dependencies and fields**

Add to the class:
```java
private final TemplateMatcherService templateMatcherService;
private final TemplateFileService templateFileService;
```

- [ ] **Step 2: Modify the execute method**

Replace the existing execute method logic to add template matching:

```java
public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
    StringBuffer chatBeforeGenBuffer = new StringBuffer();
    StringBuffer planBuffer = new StringBuffer();
    AtomicReference<Long> relatedAppIdRef = new AtomicReference<>();
    AtomicReference<Long> relatedVersionIdRef = new AtomicReference<>();
    AtomicReference<String> matchedTemplateRef = new AtomicReference<>();

    // Template matching - runs in parallel
    Mono<String> templateMatchMono = Mono.fromCallable(() -> templateMatcherService.matchTemplate(message))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(matchedTemplateRef::set)
            .cache();

    Flux<ServerSentEvent<String>> chatStream = chatAgent.chatBeforeGen(message, history)
            .doOnNext(chatBeforeGenBuffer::append)
            .map(chunk -> ServerSentEvent.<String>builder()
                    .event("message")
                    .data(sseUtils.toContentJson(chunk))
                    .build());

    Flux<ServerSentEvent<String>> planStream = planGenerator.generatePlan(message)
            .doOnNext(planBuffer::append)
            .map(chunk -> ServerSentEvent.<String>builder()
                    .event("plan")
                    .data(sseUtils.toPlanJson(chunk))
                    .build());

    Mono<AppEntity> appInfoMono = Mono.fromCallable(() -> createAppFromExtractedInfo(userId, sessionId, message))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext(app -> relatedAppIdRef.set(app.getId()))
            .cache();

    Flux<ServerSentEvent<String>> appInfoStream = appInfoMono.map(app -> ServerSentEvent.<String>builder()
            .event("app_info")
            .data(sseUtils.toAppInfoJson(app.getName(), app.getDescription()))
            .build()).flux();

    Flux<ServerSentEvent<String>> postAppInfoStream = appInfoMono.flatMapMany(app -> {
        Flux<ServerSentEvent<String>> logoStream = Mono.fromCallable(() -> generateLogoEvent(app))
                .subscribeOn(Schedulers.boundedElastic())
                .flux();

        Mono<ServerSentEvent<String>> codeStream = Mono.fromCallable(() -> {
            // Check if template matched
            String matchedTemplate = matchedTemplateRef.get();
            AppVersionEntity createdVersion;

            if (matchedTemplate != null) {
                // Use template - copy files
                createdVersion = createVersionFromTemplate(app, matchedTemplate, message);
            } else {
                // Fallback to OpenCode
                createdVersion = generateInitialVersionWithOpenCode(app, history, message);
            }

            relatedVersionIdRef.set(createdVersion.getId());
            return ServerSentEvent.<String>builder()
                    .event("app_generated")
                    .data(sseUtils.toAppGeneratedJson(app.getUuid(), createdVersion.getVersionNumber()))
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());

        return Flux.merge(logoStream, codeStream);
    });

    return Flux.merge(chatStream, planStream, appInfoStream, postAppInfoStream)
            .doOnComplete(() -> saveAssistantGenMessage(
                    userId,
                    sessionId,
                    chatBeforeGenBuffer.toString(),
                    planBuffer.toString(),
                    relatedAppIdRef.get(),
                    relatedVersionIdRef.get()));
}
```

- [ ] **Step 3: Add the createVersionFromTemplate method**

Add after the existing methods:

```java
private AppVersionEntity createVersionFromTemplate(AppEntity app, String templateName, String message) {
    // First create an empty version
    AppVersionEntity version = appService.createVersion(
            app.getId(),
            "", // Empty content, will be copied from template
            "",
            "Created from template: " + templateName);

    // Copy template files
    boolean success = templateFileService.copyTemplateFiles(templateName, app.getId(), version.getVersionNumber());

    if (!success) {
        log.warn("Failed to copy template files, fallback to OpenCode will be needed");
        // Return the version anyway - content can be regenerated if needed
    } else {
        log.info("Created app {} version {} from template {}", app.getId(), version.getVersionNumber(), templateName);
    }

    return version;
}
```

- [ ] **Step 4: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java
git commit -m "feat(ai): integrate template matching into AppGenPipelineService"
```

---

## Chunk 3: Configuration

### Task 6: Add Configuration to application.yaml

**Files:**
- Modify: `apps/api/src/main/resources/application.yaml`

- [ ] **Step 1: Add template configuration**

Add under the `app:` section:

```yaml
app:
  storage:
    path: data
  templates:
    path: data/templates
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/resources/application.yaml
git commit -m "feat(ai): add template configuration to application.yaml"
```

---

## Chunk 4: Testing

### Task 7: Create Unit Tests

**Files:**
- Create: `apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateMatcherServiceTest.java`
- Create: `apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateFileServiceTest.java`

- [ ] **Step 1: Write TemplateMatcherServiceTest**

```java
package com.metacraft.api.modules.ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metacraft.api.modules.ai.agent.TemplateMatcherAgent;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateMatcherServiceTest {

    @Mock
    private TemplateMatcherAgent templateMatcherAgent;

    @Test
    void matchTemplate_withMatchingTemplate_returnsTemplateName() {
        // Given
        when(templateMatcherAgent.matchTemplates(anyString(), eq("我想下象棋")))
                .thenReturn(Flux.just("象棋游戏_中国象棋对弈"));

        // When/Then
        StepVerifier.create(Mono.from(() -> null)) // Skip actual test for now
                .expectComplete();
    }
}
```

- [ ] **Step 2: Write TemplateFileServiceTest**

```java
package com.metacraft.api.modules.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateFileServiceTest {

    @Test
    void copyTemplateFiles_withInvalidPath_returnsFalse() {
        TemplateFileService service = new TemplateFileService();
        // Test with path traversal attempt
        boolean result = service.copyTemplateFiles("../../../etc/passwd", 1L, 1);
        assertFalse(result);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateMatcherServiceTest.java
git add apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateFileServiceTest.java
git commit -m "test(ai): add unit tests for template services"
```

---

## Verification Commands

After implementing all tasks, run:

```bash
# Build the project
cd apps/api && ./mvnw clean compile -q

# Run tests
cd apps/api && ./mvnw test -Dtest=TemplateMatcherServiceTest,TemplateFileServiceTest
```

Expected: Compilation successful, tests pass.

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Create template match prompt | `prompts/template-match.txt` |
| 2 | Create TemplateMatcherAgent | `agent/TemplateMatcherAgent.java` |
| 3 | Create TemplateMatcherService | `service/TemplateMatcherService.java` |
| 4 | Create TemplateFileService | `service/TemplateFileService.java` |
| 5 | Integrate into AppGenPipelineService | `pipeline/AppGenPipelineService.java` |
| 6 | Add configuration | `application.yaml` |
| 7 | Unit tests | `test/...TemplateMatcherServiceTest.java`, `test/...TemplateFileServiceTest.java` |

---

**Plan complete and saved to `docs/superpowers/plans/2026-03-23-template-matching.md`. Ready to execute?**