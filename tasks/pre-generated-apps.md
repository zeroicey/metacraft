# 任务：预生成应用模板功能

**状态：** 待开始 📋
**创建日期：** 2026-03-10

## 背景

在应用生成流水线中，优先使用预生成的应用模板，避免用户等待 AI 代码生成。适用于常见应用（如围棋游戏、象棋游戏等）。

## 目标

- 当用户意图为 "GEN" 时，立即检查模板库是否有匹配的应用
- 如果有匹配，直接使用模板代码，跳过 AI 代码生成
- 如果无匹配，正常执行 AI 代码生成流程

## 模板目录结构

```
data/templates/
├── 围棋游戏_一个在线围棋对弈游戏/
│   ├── index.html
│   └── app.js
├── 象棋游戏_中国象棋对弈/
│   ├── index.html
│   └── app.js
└── 五子棋_简单的五子棋游戏/
    ├── index.html
    └── app.js
```

**文件夹命名规则：** `{应用名}_{应用描述}`

## 架构设计

### 执行流程

```
UnifiedOrchestrator (intent = GEN)
    ├── [立即启动] TemplateMatcher (带 user message)
    │   └── 在后台运行，不阻塞其他流程
    │
    ├── ChatBeforeGen (并行)
    ├── PlanGenerator (并行)
    ├── AppInfoExtractor (并行)
    └── CodeGenerator (等待 AppInfo + TemplateMatch 结果)
        ├── 模板匹配成功 → 使用模板代码
        └── 模板匹配失败 → 生成新代码
```

### 时间优势

- 模板匹配与 Chat/Plan/AppInfo 并行进行
- 最坏情况：增加模板匹配的耗时（约 1-2 秒）
- 最好情况：节省整个 AI 代码生成的时间（约 10-30 秒）

## 组件设计

### TemplateMatcher.java

```java
@Service
public class TemplateMatcher {

    @AiService
    interface TemplateMatcherAI {
        @SystemMessage(fromResource = "prompts/template-match.txt")
        @UserMessage("用户需求: {{requirement}}")
        TemplateMatchResult match(@V("requirement") String requirement);
    }

    private final TemplateMatcherAI ai;
    private final String templatePath = "data/templates";

    public Mono<TemplateMatchResult> matchAsync(String userMessage) {
        // 1. 扫描模板目录
        List<Template> templates = scanTemplates();

        // 2. AI 匹配
        return Mono.fromCallable(() -> ai.match(buildContext(userMessage, templates)))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
```

### TemplateMatchResult.java

```java
public class TemplateMatchResult {
    private boolean matched;
    private String templateName;  // 匹配的模板文件夹名
    private String reason;         // 匹配原因

    // getters, setters
}
```

### Template.java

```java
public class Template {
    private String folderName;     // 文件夹名
    private String name;           // 应用名
    private String description;    // 应用描述
    private String htmlCode;
    private String jsCode;

    // getters, setters
}
```

## AI Prompt 设计

**`prompts/template-match.txt`：**

```
You are a template matching expert. Your task is to find the best matching template for user requirements.

Available templates:
{{templates}}

User requirement: {{requirement}}

Output rules (strict):
- Output must be structured output with exactly these fields:
    - matched: boolean (true if a template matches, false otherwise)
    - templateName: string (the exact folder name if matched, empty string otherwise)
    - reason: string (brief explanation of why matched or why not)
- Do NOT output explanations, markdown, or any extra text.

Matching criteria:
- Match based on semantic similarity, not exact keyword matching
- Consider the core functionality and purpose of the app
- If multiple templates could match, choose the closest one
- If no template closely matches the requirement, return matched=false

Examples:
- User: "我想做个围棋游戏" → Matched: true, templateName: "围棋游戏_一个在线围棋对弈游戏"
- User: "做一个计算器" → Matched: false (no calculator template)
- User: "围棋对战" → Matched: true, templateName: "围棋游戏_一个在线围棋对弈游戏"
```

## AppGenPipelineService 集成

**修改 `execute()` 方法：**

1. 在方法开始时启动异步模板匹配
2. 在 `codeMono` 中等待模板匹配结果
3. 根据匹配结果决定使用模板代码还是 AI 生成代码

```java
// 立即启动模板匹配（异步）
Mono<TemplateMatchResult> templateMatchMono = templateMatcher.matchAsync(message).cache();

// ... chatStream, planStream, appInfoStream ...

// 代码生成：根据模板匹配结果决定
Mono<ServerSentEvent<String>> codeMono = templateMatchMono.flatMap(matchResult -> {
    if (matchResult.isMatched()) {
        // 使用模板代码
        return useTemplateCode(app, matchResult.getTemplateName());
    } else {
        // 使用 AI 生成代码
        return useAIGeneratedCode(message, app.getName(), app.getDescription());
    }
});
```

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 模板目录不存在 | 创建空目录，继续正常生成 |
| 模板文件损坏 | 记录日志，降级到 AI 生成 |
| AI 匹配失败 | 记录日志，降级到 AI 生成 |
| 模板代码无效 | 记录日志，降级到 AI 生成 |

## 配置

**`application.yaml` 添加：**

```yaml
app:
  template-path: ${TEMPLATE_PATH:data/templates}
```

## 任务清单

### 阶段 1：基础结构
- [ ] 创建 `Template.java` 数据模型
- [ ] 创建 `TemplateMatchResult.java` DTO
- [ ] 创建 `prompts/template-match.txt` prompt

### 阶段 2：TemplateMatcher
- [ ] 创建 `TemplateMatcher.java` 服务
- [ ] 实现模板目录扫描逻辑
- [ ] 实现模板文件加载逻辑
- [ ] 实现 AI 匹配逻辑

### 阶段 3：集成
- [ ] 修改 `AppGenPipelineService.java`
- [ ] 添加配置项到 `application.yaml`
- [ ] 实现并行执行逻辑

### 阶段 4：测试
- [ ] 准备测试模板（围棋、象棋等）
- [ ] 测试模板匹配成功场景
- [ ] 测试模板匹配失败降级
- [ ] 测试边界情况（目录不存在、文件损坏等）

## 参考文件

- 流水线服务: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java`
- 统一编排器: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/UnifiedOrchestrator.java`
- 代码生成器: `apps/api/src/main/java/com/metacraft/api/modules/ai/agent/CodeGenerator.java`
