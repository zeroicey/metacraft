# 预生成应用模板功能设计

**日期：** 2026-03-17
**状态：** 已批准

## 1. 概述

在应用生成流水线中，优先使用预生成的应用模板，避免用户等待 AI 代码生成。适用于常见应用类型（如围棋游戏、象棋游戏等）。

**目标：**
- 当用户意图为 "GEN" 时，立即检查模板库是否有匹配的应用
- 如果有匹配，直接使用模板代码，跳过 AI 代码生成
- 如果无匹配，正常执行 AI 代码生成流程

---

## 2. 设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 匹配方式 | AI 语义匹配 | 灵活、精准，可理解用户需求的语义 |
| 模板目录 | `data/templates/` | 独立目录，与应用存储分离 |
| 匹配成功后处理 | 直接使用模板代码 | 速度最快，适合预置模板场景 |
| 用户提示 | 静默使用 | 不额外提示，体验更流畅 |

---

## 3. 目录结构

```
data/templates/
├── 围棋游戏_一个在线围棋对弈游戏/
│   ├── index.html
│   └── app.js
├── 象棋游戏_中国象棋对弈/
│   ├── index.html
│   └── app.js
└── ...
```

**文件夹命名规则：** `{应用名}_{应用描述}`

---

## 4. 组件设计

### 4.1 Template 数据模型

```java
public class Template {
    private String folderName;        // 文件夹名
    private String appName;          // 应用名（从文件夹名提取）
    private String description;      // 描述（从文件夹名提取）
    private String htmlContent;      // index.html 内容
    private String jsContent;        // app.js 内容
}
```

### 4.2 TemplateMatchResult DTO

```java
public class TemplateMatchResult {
    private boolean matched;        // 是否匹配成功
    private String templateName;    // 匹配的模板文件夹名
    private String reason;          // 匹配原因
    private Template template;      // 匹配的模板对象（matched为true时包含完整模板内容）
}
```

### 4.3 TemplateMatcher 服务

**职责：**
- 扫描模板目录，加载模板文件
- 调用 AI 进行语义匹配
- 返回匹配结果

**核心方法：**

```java
@Service
public class TemplateMatcher {
    // 扫描并缓存所有模板（应用启动时加载一次）
    public List<Template> scanTemplates();

    // 异步匹配用户需求与模板（返回 Mono，可设置超时）
    public Mono<TemplateMatchResult> matchAsync(String userMessage);

    // 同步匹配（带超时保护）
    public Mono<TemplateMatchResult> matchWithTimeout(String userMessage, Duration timeout);
}
```

**超时配置：**
- 模板匹配默认超时：3 秒
- 可通过配置 `app.template-match-timeout` 调整

**缓存策略：**
- 模板列表在应用启动时扫描一次，使用内存缓存
- 使用 `@PostConstruct` 初始化模板缓存
- 线程安全：使用 `Collections.unmodifiableList()` 保护模板列表

---

## 5. 执行流程

```
AppGenPipelineService.execute()
    │
    ├── [立即启动] templateMatcher.matchAsync(userMessage)
    │              .timeout(Duration.ofSeconds(3))
    │              .onErrorReturn(TemplateMatchResult.noMatch())
    │                                                      │
    ├── chatAgent.chatBeforeGen() (并行)                   │
    ├── planGenerator.generatePlan() (并行)                │
    ├── appInfoExtractor.extract() (并行)                   │
    │                                                      │
    └── postAppInfoStream 中检查模板匹配结果
            │
            ├── 模板匹配成功 → 直接使用模板代码 → 保存版本
            │
            └── 模板未匹配/超时 → 调用 OpenCode 生成代码 → 保存版本
```

**响应式集成说明：**
- 使用 `Mono.cache()` 缓存模板匹配结果
- 在 `postAppInfoStream` 阶段通过 `.flatMap()` 同步等待模板匹配结果
- 设置超时保护，确保不会阻塞整体流程
- 匹配超时视为"未匹配"，降级到 OpenCode 生成

**时间优势：**
- 模板匹配与 Chat/Plan/AppInfo 并行进行
- 最坏情况：增加模板匹配的耗时（约 1-2 秒）
- 最好情况：节省整个 AI 代码生成的时间（约 10-30 秒）

---

## 6. 集成修改

### 6.1 AppGenPipelineService

修改 `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/AppGenPipelineService.java`：

1. 注入 `TemplateMatcher`
2. 在 `execute()` 开始时启动异步模板匹配
3. 新增 `generateCodeWithTemplate()` 方法处理模板匹配逻辑
4. 匹配成功时直接保存模板代码，跳过 OpenCode 调用

### 6.2 配置项

在 `application.yaml` 添加：

```yaml
app:
  template-path: ${TEMPLATE_PATH:data/templates}
  template-match-timeout: ${TEMPLATE_MATCH_TIMEOUT:3}  # 秒
```

---

## 7. Prompt 设计

创建 `prompts/template-match.txt`：

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

---

## 8. 错误处理

| 场景 | 处理方式 |
|------|----------|
| 模板目录不存在 | 创建空目录，记录日志，继续正常生成 |
| 模板文件损坏（无法解析） | 记录日志，跳过该模板 |
| AI 匹配超时 | 记录日志，返回未匹配，降级到 OpenCode 生成 |
| AI 匹配失败（异常） | 记录日志，返回未匹配，降级到 OpenCode 生成 |
| AI 返回无效 JSON | 记录日志，返回未匹配，降级到 OpenCode 生成 |
| 模板代码无效（保存失败） | 记录日志，降级到 OpenCode 生成 |

---

## 9. 文件变更清单

### 新增文件

| 文件路径 | 说明 |
|----------|------|
| `modules/ai/entity/Template.java` | 模板数据模型 |
| `modules/ai/dto/TemplateMatchResult.java` | 匹配结果 DTO |
| `modules/ai/service/TemplateMatcher.java` | 模板匹配服务 |
| `resources/prompts/template-match.txt` | AI 匹配 Prompt |

### 修改文件

| 文件路径 | 说明 |
|----------|------|
| `modules/ai/service/pipeline/AppGenPipelineService.java` | 集成模板匹配逻辑 |
| `resources/application.yaml` | 添加配置项 |

---

## 10. 测试计划

### 阶段 1：基础功能测试

| 测试用例 | 预期结果 |
|----------|----------|
| 准备测试模板（围棋、象棋等） | 模板文件正确加载 |
| 测试模板目录扫描功能 | 所有模板被正确识别 |
| 测试 AI 匹配功能 - 用户请求"围棋游戏" | 匹配成功，返回模板名称 |
| 测试 AI 匹配功能 - 用户请求"做一个五子棋" | 匹配失败，返回未匹配 |

### 阶段 2：集成测试

| 测试用例 | 预期结果 |
|----------|----------|
| 模板匹配成功场景 | 直接使用模板代码，跳过 OpenCode，响应时间短 |
| 模板匹配失败降级 | 正常调用 OpenCode 生成代码 |
| 模板匹配超时 | 降级到 OpenCode 生成，记录警告日志 |

### 阶段 3：边界测试

| 测试用例 | 预期结果 |
|----------|----------|
| 模板目录不存在 | 创建空目录，继续正常生成 |
| 模板文件损坏（缺少 index.html） | 跳过损坏模板，记录日志 |
| AI 服务不可用 | 降级到 OpenCode 生成，记录错误日志 |
| 并发请求多个用户匹配 | 缓存模板列表，线程安全处理 |