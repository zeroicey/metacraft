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
    private String reason;           // 匹配原因
}
```

### 4.3 TemplateMatcher 服务

**职责：**
- 扫描模板目录，加载模板文件
- 调用 AI 进行语义匹配
- 返回匹配结果

**核心方法：**
- `scanTemplates()`: 扫描并加载所有模板
- `matchAsync(String userMessage)`: 异步匹配用户需求与模板

---

## 5. 执行流程

```
AppGenPipelineService.execute()
    │
    ├── [立即启动] templateMatcher.matchAsync(userMessage)  ──┐
    │                                                        │
    ├── chatAgent.chatBeforeGen()                            │
    ├── planGenerator.generatePlan()                        │
    ├── appInfoExtractor.extract()                          │
    │                                                        │
    └── generateCodeWithTemplate() ◄────────────────────────┘
            │
            ├── 模板匹配成功 → 直接使用模板代码 → 保存版本
            │
            └── 模板未匹配 → 调用 OpenCode 生成代码 → 保存版本
```

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
```

---

## 8. 错误处理

| 场景 | 处理方式 |
|------|----------|
| 模板目录不存在 | 创建空目录，记录日志，继续正常生成 |
| 模板文件损坏（无法解析） | 记录日志，跳过该模板 |
| AI 匹配失败 | 记录日志，返回未匹配，降级到 OpenCode 生成 |
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
- 准备测试模板（围棋、象棋等）
- 测试模板目录扫描功能
- 测试 AI 匹配功能

### 阶段 2：集成测试
- 测试模板匹配成功场景
- 测试模板匹配失败降级到 OpenCode

### 阶段 3：边界测试
- 模板目录不存在
- 模板文件损坏
- AI 服务不可用