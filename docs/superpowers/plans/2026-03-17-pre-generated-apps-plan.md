# 预生成应用模板功能实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在应用生成流水线中，优先使用预生成的应用模板，避免用户等待 AI 代码生成

**Architecture:** 模板匹配作为 AppGenPipelineService 的并行步骤，匹配成功后直接使用模板代码，跳过 OpenCode 调用

**Tech Stack:** Spring Boot, LangChain4j, Reactive (Flux/Mono)

---

## 文件结构

```
新增文件:
- apps/api/src/main/java/com/metacraft/api/modules/ai/entity/Template.java
- apps/api/src/main/java/com/metacraft/api/modules/ai/dto/TemplateMatchResult.java
- apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatcher.java
- apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatchService.java (AI接口)
- apps/api/src/main/resources/prompts/template-match.txt

修改文件:
- apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java
- apps/api/src/main/resources/application.yaml
```

---

## Chunk 1: 数据模型和 DTO

### Task 1: Template 实体类

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/entity/Template.java`
- Test: `apps/api/src/test/java/com/metacraft/api/modules/ai/entity/TemplateTest.java`

- [ ] **Step 1: 写测试**

```java
package com.metacraft.api.modules.ai.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateTest {

    @Test
    void testTemplateCreation() {
        Template template = new Template();
        template.setFolderName("围棋游戏_一个在线围棋对弈游戏");
        template.setAppName("围棋游戏");
        template.setDescription("一个在线围棋对弈游戏");
        template.setHtmlContent("<html>...</html>");
        template.setJsContent("// js code");

        assertEquals("围棋游戏_一个在线围棋对弈游戏", template.getFolderName());
        assertEquals("围棋游戏", template.getAppName());
        assertEquals("一个在线围棋对弈游戏", template.getDescription());
    }

    @Test
    void testParseFromFolderName() {
        Template template = Template.fromFolderName("围棋游戏_一个在线围棋对弈游戏");

        assertEquals("围棋游戏", template.getAppName());
        assertEquals("一个在线围棋对弈游戏", template.getDescription());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=TemplateTest -pl apps/api`
Expected: FAIL (class not found)

- [ ] **Step 3: 实现 Template 实体类**

```java
package com.metacraft.api.modules.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    private String folderName;        // 文件夹名
    private String appName;          // 应用名
    private String description;      // 描述
    private String htmlContent;      // index.html 内容
    private String jsContent;        // app.js 内容

    public static Template fromFolderName(String folderName) {
        Template template = new Template();
        template.setFolderName(folderName);

        // 解析文件夹名: {应用名}_{描述}
        int underscoreIndex = folderName.indexOf('_');
        if (underscoreIndex > 0) {
            template.setAppName(folderName.substring(0, underscoreIndex));
            template.setDescription(folderName.substring(underscoreIndex + 1));
        } else {
            template.setAppName(folderName);
            template.setDescription("");
        }

        return template;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=TemplateTest -pl apps/api`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/entity/Template.java
git add apps/api/src/test/java/com/metacraft/api/modules/ai/entity/TemplateTest.java
git commit -m "feat: add Template entity class

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: TemplateMatchResult DTO

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/dto/TemplateMatchResult.java`
- Test: `apps/api/src/test/java/com/metacraft/api/modules/ai/dto/TemplateMatchResultTest.java`

- [ ] **Step 1: 写测试**

```java
package com.metacraft.api.modules.ai.dto;

import com.metacraft.api.modules.ai.entity.Template;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateMatchResultTest {

    @Test
    void testNoMatchResult() {
        TemplateMatchResult result = TemplateMatchResult.noMatch();

        assertFalse(result.isMatched());
        assertNull(result.getTemplateName());
        assertNull(result.getTemplate());
    }

    @Test
    void testMatchedResult() {
        Template template = Template.fromFolderName("围棋游戏_一个在线围棋对弈游戏");
        template.setHtmlContent("<html>");
        template.setJsContent("console.log('test')");

        TemplateMatchResult result = TemplateMatchResult.matched(template, "匹配成功");

        assertTrue(result.isMatched());
        assertEquals("围棋游戏_一个在线围棋对弈游戏", result.getTemplateName());
        assertEquals("匹配成功", result.getReason());
        assertNotNull(result.getTemplate());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=TemplateMatchResultTest -pl apps/api`
Expected: FAIL

- [ ] **Step 3: 实现 TemplateMatchResult DTO**

```java
package com.metacraft.api.modules.ai.dto;

import com.metacraft.api.modules.ai.entity.Template;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMatchResult {
    private boolean matched;
    private String templateName;
    private String reason;
    private Template template;

    public static TemplateMatchResult noMatch() {
        return new TemplateMatchResult(false, null, null, null);
    }

    public static TemplateMatchResult matched(Template template, String reason) {
        return new TemplateMatchResult(
            true,
            template.getFolderName(),
            reason,
            template
        );
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=TemplateMatchResultTest -pl apps/api`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/dto/TemplateMatchResult.java
git add apps/api/src/test/java/com/metacraft/api/modules/ai/dto/TemplateMatchResultTest.java
git commit -m "feat: add TemplateMatchResult DTO

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 2: Prompt 和服务接口

### Task 3: template-match prompt 文件

**Files:**
- Create: `apps/api/src/main/resources/prompts/template-match.txt`

- [ ] **Step 1: 创建 prompt 文件**

```
你是一个模板匹配助手，擅长判断用户需求与预置模板的匹配度。

## 任务
分析用户需求与可用模板的语义相似度，返回结构化匹配结果。

## 可用模板列表
{{templateList}}

## 用户需求
{{userMessage}}

## 输出要求
请返回 JSON 格式的匹配结果：
{
  "matched": true/false,
  "templateName": "模板文件夹名（matched为true时必填）",
  "reason": "匹配原因说明"
}

## 匹配规则
- 仅当用户需求与模板描述高度语义相似时返回 matched: true
- 如果用户需求是创建某个具体应用，且模板能直接满足，返回 matched: true
- 如果用户需求需要定制开发或模板无法满足，返回 matched: false
- 不要过度匹配，避免给用户不相关的模板

## 重要
- 必须返回合法的 JSON 格式，不要返回其他内容
- 如果无法确定匹配，返回 matched: false
```

- [ ] **Step 2: 提交**

```bash
git add apps/api/src/main/resources/prompts/template-match.txt
git commit -m "feat: add template-match prompt

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: TemplateMatchService AI 接口

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatchService.java`
- Test: `apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateMatchServiceTest.java`

- [ ] **Step 1: 先查看现有 AI 服务模式**

Read: `apps/api/src/main/java/com/metacraft/api/modules/ai/agent/ChatAgent.java`

- [ ] **Step 2: 写测试（模拟测试，不实际调用 AI）**

```java
package com.metacraft.api.modules.ai.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateMatchServiceTest {

    @Test
    void testServiceExists() {
        // 验证服务接口存在
        assertNotNull(TemplateMatchService.class);
    }
}
```

- [ ] **Step 3: 实现 TemplateMatchService 接口**

```java
package com.metacraft.api.modules.ai.service;

import org.springframework.stereotype.Service;
import dev.langchain4j.service.AiService;
import com.metacraft.api.modules.ai.service.pipeline.TemplateMatchService;
import reactor.core.publisher.Flux;

@AiService
public interface TemplateMatchService {
    @dev.langchain4j.service.SystemMessage(fromResource = "prompts/template-match.txt")
    Flux<String> matchTemplates(@dev.langchain4j.service.UserMessage String prompt);
}
```

注意：需要修复包路径，应该放在 agent 目录或其他合适位置。LangChain4j @AiService 需要一个实现类。

- [ ] **Step 4: 提交**

```bash
git commit -m "feat: add TemplateMatchService interface

Note: Will implement with actual AI client in TemplateMatcher

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 3: TemplateMatcher 服务

### Task 5: TemplateMatcher 服务类

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatcher.java`
- Test: `apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateMatcherTest.java`

- [ ] **Step 1: 写测试**

```java
package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.entity.Template;
import com.metacraft.api.modules.ai.dto.TemplateMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateMatcherTest {

    private TemplateMatcher templateMatcher;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        templateMatcher = new TemplateMatcher();
        ReflectionTestUtils.setField(templateMatcher, "templatePath", tempDir.toString());
        ReflectionTestUtils.setField(templateMatcher, "timeoutSeconds", 3);
    }

    @Test
    void testScanEmptyTemplate() {
        templateMatcher.init();

        List<Template> templates = templateMatcher.getTemplates();
        assertNotNull(templates);
        assertTrue(templates.isEmpty());
    }

    @Test
    void testParseFolderName() {
        Template template = Template.fromFolderName("围棋游戏_一个在线围棋对弈游戏");

        assertEquals("围棋游戏", template.getAppName());
        assertEquals("一个在线围棋对弈游戏", template.getDescription());
    }

    @Test
    void testNoMatchWhenNoTemplates() {
        templateMatcher.init();

        TemplateMatchResult result = templateMatcher.matchSync("做一个围棋游戏");

        assertFalse(result.isMatched());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=TemplateMatcherTest -pl apps/api`
Expected: FAIL (class not found)

- [ ] **Step 3: 实现 TemplateMatcher 服务**

```java
package com.metacraft.api.modules.ai.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.ai.dto.TemplateMatchResult;
import com.metacraft.api.modules.ai.entity.Template;

import dev.langchain4j.service.AiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateMatcher {

    @Value("${app.template-path:data/templates}")
    private String templatePath;

    @Value("${app.template-match-timeout:3}")
    private int timeoutSeconds;

    private final TemplateMatchAiService templateMatchAiService;

    private List<Template> templateCache = Collections.emptyList();

    @PostConstruct
    public void init() {
        scanAndLoadTemplates();
    }

    public void scanAndLoadTemplates() {
        try {
            Path templateDir = Paths.get(templatePath).toAbsolutePath().normalize();

            if (!Files.exists(templateDir)) {
                Files.createDirectories(templateDir);
                log.info("Created template directory: {}", templateDir);
                templateCache = Collections.emptyList();
                return;
            }

            try (Stream<Path> paths = Files.walk(templateDir, 1)) {
                templateCache = paths
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(templateDir))
                    .map(this::loadTemplateFromDirectory)
                    .filter(t -> t != null)
                    .toList();
            }

            log.info("Loaded {} templates from {}", templateCache.size(), templatePath);
        } catch (IOException e) {
            log.error("Failed to scan template directory: {}", templatePath, e);
            templateCache = Collections.emptyList();
        }
    }

    private Template loadTemplateFromDirectory(Path dir) {
        try {
            String folderName = dir.getFileName().toString();
            Template template = Template.fromFolderName(folderName);

            Path indexHtml = dir.resolve("index.html");
            if (Files.exists(indexHtml)) {
                template.setHtmlContent(Files.readString(indexHtml));
            }

            Path appJs = dir.resolve("app.js");
            if (Files.exists(appJs)) {
                template.setJsContent(Files.readString(appJs));
            }

            return template;
        } catch (IOException e) {
            log.warn("Failed to load template from {}: {}", dir, e.getMessage());
            return null;
        }
    }

    public List<Template> getTemplates() {
        return Collections.unmodifiableList(templateCache);
    }

    public Mono<TemplateMatchResult> matchAsync(String userMessage) {
        if (templateCache.isEmpty()) {
            return Mono.just(TemplateMatchResult.noMatch());
        }

        String templateList = buildTemplateListPrompt();
        String prompt = String.format("可用模板列表:\n%s\n\n用户需求:\n%s", templateList, userMessage);

        return Flux.from(templateMatchAiService.matchTemplates(prompt))
                .reduce(String::concat)
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::parseMatchResult)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .onErrorResume(e -> {
                    log.warn("Template matching failed: {}", e.getMessage());
                    return Mono.just(TemplateMatchResult.noMatch());
                });
    }

    public Mono<TemplateMatchResult> matchWithTimeout(String userMessage, Duration timeout) {
        return matchAsync(userMessage)
                .timeout(timeout)
                .onErrorResume(e -> {
                    log.warn("Template matching timed out: {}", e.getMessage());
                    return Mono.just(TemplateMatchResult.noMatch());
                });
    }

    public TemplateMatchResult matchSync(String userMessage) {
        return matchAsync(userMessage).block();
    }

    private String buildTemplateListPrompt() {
        StringBuilder sb = new StringBuilder();
        for (Template template : templateCache) {
            sb.append("- ").append(template.getFolderName())
              .append(" - ").append(template.getDescription()).append("\n");
        }
        return sb.toString();
    }

    private TemplateMatchResult parseMatchResult(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonResponse);

            boolean matched = node.has("matched") && node.get("matched").asBoolean();
            if (!matched) {
                return TemplateMatchResult.noMatch();
            }

            String templateName = node.has("templateName") ? node.get("templateName").asText() : null;
            String reason = node.has("reason") ? node.get("reason").asText() : null;

            Template matchedTemplate = templateCache.stream()
                    .filter(t -> t.getFolderName().equals(templateName))
                    .findFirst()
                    .orElse(null);

            if (matchedTemplate != null) {
                return TemplateMatchResult.matched(matchedTemplate, reason);
            }

            return TemplateMatchResult.noMatch();
        } catch (Exception e) {
            log.warn("Failed to parse template match result: {}", e.getMessage());
            return TemplateMatchResult.noMatch();
        }
    }

    // AI Service 接口
    @AiService
    public interface TemplateMatchAiService {
        @dev.langchain4j.service.SystemMessage(fromResource = "prompts/template-match.txt")
        Flux<String> matchTemplates(@dev.langchain4j.service.UserMessage String prompt);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=TemplateMatcherTest -pl apps/api`
Expected: PASS (或需要调整测试)

- [ ] **Step 5: 提交**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/service/TemplateMatcher.java
git add apps/api/src/test/java/com/metacraft/api/modules/ai/service/TemplateMatcherTest.java
git commit -m "feat: add TemplateMatcher service

- Template scanning and loading from directory
- AI-powered semantic matching
- Timeout and error handling
- Reactive API with Mono/Flux

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 4: 集成到 AppGenPipelineService

### Task 6: 添加配置项

**Files:**
- Modify: `apps/api/src/main/resources/application.yaml`

- [ ] **Step 1: 查看现有配置**

Read: `apps/api/src/main/resources/application.yaml` (前 50 行)

- [ ] **Step 2: 添加模板配置**

```yaml
app:
  template-path: ${TEMPLATE_PATH:data/templates}
  template-match-timeout: ${TEMPLATE_MATCH_TIMEOUT:3}
```

- [ ] **Step 3: 提交**

```bash
git add apps/api/src/main/resources/application.yaml
git commit -m "config: add template matching configuration

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: 集成到 AppGenPipelineService

**Files:**
- Modify: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java`

- [ ] **Step 1: 查看现有代码**

Read: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java`

- [ ] **Step 2: 修改 - 注入 TemplateMatcher**

在类顶部添加字段:
```java
private final TemplateMatcher templateMatcher;
```

在构造函数中添加参数:
```java
public AppGenPipelineService(
    // ... existing fields
    TemplateMatcher templateMatcher  // 添加这行
) {
    // ... existing assignments
    this.templateMatcher = templateMatcher;
}
```

- [ ] **Step 3: 修改 - 添加模板匹配逻辑**

在 execute 方法中，appInfoMono 之后添加:

```java
// 模板匹配 - 立即启动，与其他流程并行
AtomicReference<TemplateMatchResult> templateMatchRef = new AtomicReference<>();

Mono<TemplateMatchResult> templateMatchMono = Mono.fromCallable(() ->
        templateMatcher.matchWithTimeout(message, Duration.ofSeconds(3))
    ).subscribeOn(Schedulers.boundedElastic())
    .cache();

// 修改 postAppInfoStream 部分
Flux<ServerSentEvent<String>> postAppInfoStream = appInfoMono.flatMapMany(app -> {
    Flux<ServerSentEvent<String>> logoStream = Mono.fromCallable(() -> generateLogoEvent(app))
            .subscribeOn(Schedulers.boundedElastic())
            .flux();

    // 检查模板匹配结果
    Flux<ServerSentEvent<String>> codeStream = templateMatchMono.flatMapMany(matchResult -> {
        templateMatchRef.set(matchResult);

        if (matchResult.isMatched()) {
            // 使用模板代码
            return Mono.fromCallable(() -> {
                AppVersionEntity version = appService.createVersion(
                    app.getId(),
                    matchResult.getTemplate().getHtmlContent(),
                    matchResult.getTemplate().getJsContent(),
                    "使用模板: " + matchResult.getTemplateName()
                );
                relatedVersionIdRef.set(version.getId());
                return ServerSentEvent.<String>builder()
                    .event("app_generated")
                    .data(sseUtils.toAppGeneratedJson(app.getUuid(), version.getVersionNumber()))
                    .build();
            }).subscribeOn(Schedulers.boundedElastic()).flux();
        } else {
            // 降级到 OpenCode 生成
            return Mono.fromCallable(() -> {
                AppVersionEntity createdVersion = generateInitialVersionWithOpenCode(app, history, message);
                relatedVersionIdRef.set(createdVersion.getId());
                return ServerSentEvent.<String>builder()
                    .event("app_generated")
                    .data(sseUtils.toAppGeneratedJson(app.getUuid(), createdVersion.getVersionNumber()))
                    .build();
            }).subscribeOn(Schedulers.boundedElastic()).flux();
        }
    });

    return Flux.merge(logoStream, codeStream);
});
```

- [ ] **Step 4: 添加必要的 import**

```java
import com.metacraft.api.modules.ai.dto.TemplateMatchResult;
import com.metacraft.api.modules.ai.service.TemplateMatcher;
import java.time.Duration;
```

- [ ] **Step 5: 运行测试验证**

Run: `./mvnw test -Dtest=AppGenPipelineServiceTest -pl apps/api` (如果有的话)
或: `./mvnw compile -pl apps/api`

- [ ] **Step 6: 提交**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java
git commit -m "feat: integrate template matching into AppGenPipelineService

- Add TemplateMatcher as dependency
- Parallel template matching with timeout
- Fallback to OpenCode when no match
- Use template code directly on match

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 5: 测试模板

### Task 8: 创建测试模板

**Files:**
- Create: `data/templates/围棋游戏_一个在线围棋对弈游戏/index.html`
- Create: `data/templates/围棋游戏_一个在线围棋对弈游戏/app.js`
- Create: `data/templates/象棋游戏_中国象棋对弈/index.html`
- Create: `data/templates/象棋游戏_中国象棋对弈/app.js`

- [ ] **Step 1: 创建测试模板目录和文件**

```bash
mkdir -p data/templates/围棋游戏_一个在线围棋对弈游戏
mkdir -p data/templates/象棋游戏_中国象棋对弈
```

- [ ] **Step 2: 创建 index.html 模板**

创建简单的 HTML 模板（可以用占位符）

- [ ] **Step 3: 提交**

```bash
git add data/templates/
git commit -m "test: add test templates for围棋 and 象棋

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 总结

此计划包含 8 个任务，分布在 5 个 Chunk 中：

| Chunk | 任务 | 说明 |
|-------|------|------|
| 1 | Task 1-2 | 数据模型和 DTO |
| 2 | Task 3-4 | Prompt 和 AI 服务接口 |
| 3 | Task 5 | TemplateMatcher 服务 |
| 4 | Task 6-7 | 配置和集成 |
| 5 | Task 8 | 测试模板 |

计划完成后，所有文件变更:
- 新增: 5 个 Java 文件, 1 个 prompt 文件, 测试模板
- 修改: 2 个现有文件 (AppGenPipelineService, application.yaml)