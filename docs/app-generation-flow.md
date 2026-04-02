# 应用生成流程设计

本文档描述 MetaCraft 后端「生成应用」功能的完整流程设计。

---

## 整体流程概览

```
┌─────────────────────────────────────────────────────────────────┐
│  Step 1: 意图判断                                               │
│  用户消息 → Intent Agent → 判断结果                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓ (Generation)
┌─────────────────────────────────────────────────────────────────┐
│  Step 2: 模板匹配 (Template Matcher)                            │
│  用户消息 → 模板目录扫描 → AI 匹配 → 返回模板名称 或 null       │
│                                                                 │
│  ┌─────────────────────────┐  ┌─────────────────────────┐      │
│  │  匹配到模板?            │  │  没有匹配到模板?         │      │
│  │    ↓ 是                 │  │       ↓ 是               │      │
│  │  跳过架构设计           │  │  进入 Step 3 手动生成    │      │
│  │  直接复制模板文件      │  │                          │      │
│  └─────────────────────────┘  └─────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 3: 并行执行初始任务                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐        │
│  │ Chat before │  │  生成 Plan  │  │ 生成应用信息    │        │
│  │     Gen     │  │  (给前端)   │  │ (name+desc)     │        │
│  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘        │
│         │                │                  │                  │
│         └────────────────┴──────────────────┘                  │
│                          ↓                                      │
│                   SSE: message + plan + app_info               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 4: 并行执行后续任务 (等待 app_info 完成)                  │
│                                                                 │
│  应用信息 (name) ┬─→ Logo 生成                                  │
│                 └─→ 架构设计 (Architect Agent)                  │
│                          ↓                                      │
│                   SSE: logo_generated + blueprint              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 5: 契约设计                                               │
│  蓝图 (file_list) → Contract Agent → 接口契约                   │
│                          ↓                                      │
│                   SSE: contract                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 6: 文件生成                                               │
│  契约 + file_list → N 个 Code Agent → N 个代码文件              │
│                          ↓                                      │
│           SSE: code_file (index.html, store.js, ...)           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Step 7: 完成                                                   │
│  SSE: app_generated → done                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Step 1: 意图判断

### 输入
- 用户消息 (String message)

### 处理
```
Intent Agent.analyze(message)
    │
    ├──→ 判断结果: CHAT / GENERATION / EDIT
    │
    └──→ 如果是 GENERATION，继续 Step 2
```

### 输出
- 意图分类结果 (Enum: CHAT, GENERATION, EDIT)

### SSE 事件
```
event: intent
data: "generation"
```

---

## Step 2: 模板匹配 (Template Matcher)

### 概述

在正式生成应用之前，系统会先检查是否存在预先准备好的模板应用。如果用户的需求与某个模板匹配，则直接使用该模板，跳过后续的架构设计和代码生成步骤。

### 输入
- 用户消息 (message)

### 处理

```
┌─────────────────────────────────────────────────────────────┐
│  1. 扫描模板目录                                             │
│  - 读取 templatesPath 目录下的所有子文件夹                    │
│  - 返回模板名称列表: [template1, template2, ...]            │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  2. AI 匹配                                                   │
│  - 将模板名称列表和用户消息发送给 TemplateMatcherAgent       │
│  - Agent 分析用户需求，返回最匹配的模板名称                   │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  3. 解析结果                                                  │
│  - 如果返回 "NONE" 或空 → 没有匹配到模板                     │
│  - 如果返回模板名称 → 匹配成功                               │
│  - 验证返回的模板名称是否存在于目录中                         │
└─────────────────────────────────────────────────────────────┘
```

### 核心逻辑

```java
// 伪代码
public Mono<String> matchTemplate(String userMessage) {
    // 1. 扫描模板目录
    List<String> templateNames = scanTemplateDirectories();

    if (templateNames.isEmpty()) {
        return Mono.empty();  // 没有模板，跳过匹配
    }

    // 2. 调用 AI 匹配
    String templatesList = String.join("\n", templateNames);
    return templateMatcherAgent.matchTemplates(templatesList, userMessage)
        .map(response -> parseTemplateMatchResponse(response, templateNames));
}

// 解析匹配结果
private String parseTemplateMatchResponse(String response, List<String> templates) {
    if (response == null || response.isBlank() || response.equalsIgnoreCase("NONE")) {
        return null;  // 没有匹配
    }

    // 验证模板是否存在
    if (templates.contains(response.trim())) {
        return response.trim();  // 返回匹配的模板名称
    }

    return null;  // 模板不存在
}
```

### 分支处理

```
用户消息
    ↓
Template Matcher
    ↓
    ├─→ 匹配到模板 → 使用模板 → 跳过架构设计 → 直接复制模板文件
    │
    └─→ 没有匹配 → 继续手动生成流程 → 进入 Step 3
```

### 输出

| 情况 | 输出 | 后续处理 |
|------|------|----------|
| 匹配到模板 | 模板名称 (String) | 复制模板文件到应用目录 |
| 没有匹配 | null | 继续 Step 3 手动生成 |

### 组件

| 组件 | 职责 | 输入 | 输出 |
|------|------|------|------|
| TemplateMatcherService | 模板匹配服务 | userMessage | 模板名称或null |
| TemplateMatcherAgent | AI 匹配 Agent | templatesList, message | 匹配结果 |
| TemplateFileService | 模板文件复制 | templateName, appId, version | 复制结果 |

### 配置文件

```yaml
# application.yaml
app:
  templates:
    path: data/templates  # 模板目录路径
    matched-response-delay-ms: 3000  # 匹配响应延迟(毫秒)
```

### 提示词

`prompts/template-match.txt`:
```
Analyze the user's request and match it against the provided template list.
Only return the template name if there's a clear match.
If no template matches well, return exactly "NONE".
Do not add any explanation or additional text.
```

### 重要特性

1. **并行执行**: 模板匹配与 Step 3 的初始任务并行执行
2. **容错处理**: 如果匹配出错或超时，静默降级到手动生成
3. **验证机制**: 返回的模板名称必须存在于目录中，否则视为无效
4. **延迟响应**: 匹配到模板后，可以添加延迟让用户体验更好

---

## Step 3: 并行执行初始任务

### 输入
- 用户消息 (message)
- 会话历史 (history)

### 处理 (并行执行)

```
┌─────────────────────────────────────────────────────────────┐
│  Parallel Execution (无依赖，可同时执行)                      │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Agent A: Chat before Gen                             │    │
│  │ Input:  message, history                            │    │
│  │ Prompt: gen-chat.txt (100字温暖回复)                │    │
│  │ Output: SSE event:message                          │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Agent B: 生成 Plan                                  │    │
│  │ Input:  message                                     │    │
│  │ Prompt: gen-plan.txt (4-8条实施计划)               │    │
│  │ Output: SSE event:plan                             │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Agent C: 生成应用信息                                │    │
│  │ Input:  message                                     │    │
│  │ Prompt: gen-app-info.txt                           │    │
│  │ Output: AppInfoDTO { name, description }          │    │
│  │        + 创建 AppEntity 到数据库                   │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 关键点
- 三个 Agent 完全独立，无依赖关系
- 谁先返回就先发送对应的 SSE 事件
- Chat before Gen 和 Plan 只给前端展示用，不影响后续流程
- 应用信息需要保存到数据库，返回 app_id

### 输出

| SSE Event | 内容 |
|-----------|------|
| message | 开场白文本 |
| plan | 实施计划 (bullet list) |
| app_info | `{name, description, app_id}` |

---

## Step 4: 并行执行后续任务

### 依赖
- 必须等待 Step 2 的 app_info 完成 (获取 name)

### 输入
- 用户消息 (message)
- 应用名称 (name) - 从 app_info 获取
- 应用描述 (description) - 从 app_info 获取
- 应用实体 (app) - 包含 app_id

### 处理 (并行执行)

```
┌─────────────────────────────────────────────────────────────┐
│  Parallel Execution (等待 app_info 完成后)                   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Task A: Logo 生成                                    │    │
│  │ Input:  app entity                                   │    │
│  │ 调用:  ImageService.generateLogoAndSave()           │    │
│  │ Output: SSE event:logo_generated                    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Agent B: 架构设计 (Architect Agent)                 │    │
│  │ Input:  message, name, description                  │    │
│  │ Prompt: gen-architect.txt                           │    │
│  │ Output: Blueprint { file_list }                     │    │
│  │        = [                                           │    │
│  │          {file_id: "index", file_path: "...", ... } │    │
│  │          {file_id: "js_store", file_path: "...", }  │    │
│  │          ...                                         │    │
│  │        ]                                             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 关键点
- 两者都依赖 app_info 的完成
- Logo 生成是独立的，不阻塞架构设计
- 架构设计需要使用确定的 name，避免冲突

### 输出

| SSE Event | 内容 |
|-----------|------|
| logo_generated | `{logoUuid, ext}` |
| blueprint | `{project_blueprint: {file_list: [...]}}` |

---

## Step 5: 契约设计

### 依赖
- 必须等待 Step 3 的 blueprint 完成 (获取 file_list)

### 输入
- 蓝图 (blueprint) - 包含 file_list

### 处理

```
┌─────────────────────────────────────────────────────────────┐
│  Agent: API 契约设计师 (Contract Agent)                     │
│                                                              │
│  Input:  blueprint (JSON 格式)                              │
│  Prompt: gen-contract.txt                                   │
│                                                              │
│  Task: 为 file_list 中每个文件定义全局接口契约              │
│        - 暴露的变量名                                        │
│        - 函数签名                                            │
│        - 参数和返回值类型                                     │
│                                                              │
│  Output: Contract (Markdown 格式)                           │
│                                                              │
│  Example:                                                    │
│  # 全局接口契约                                              │
│                                                              │
│  ## store.js                                                 │
│  - 暴露: const store = Vue.reactive({...})                  │
│  - 暴露: function commit(action, payload)                  │
│                                                              │
│  ## api.js                                                   │
│  - 暴露: async function loadData() => Array                 │
│  - 暴露: async function saveData(data) => boolean           │
└─────────────────────────────────────────────────────────────┘
```

### 输出

| SSE Event | 内容 |
|-----------|------|
| contract | Markdown 格式的接口契约文档 |

---

## Step 6: 文件生成

### 依赖
- 必须等待 Step 4 的 contract 完成

### 输入
- file_list (从 blueprint 获取)
- contract (从 Contract Agent 获取)

### 处理

```
┌─────────────────────────────────────────────────────────────┐
│  根据 file_list 数量，开启 N 个并行的 Code Agent            │
│                                                              │
│  file_list = [                                              │
│    {file_id: "index", file_path: "index.html", ...},         │
│    {file_id: "js_store", file_path: "store.js", ...},      │
│    {file_id: "js_api", file_path: "api.js", ...},           │
│    {file_id: "js_app", file_path: "app.js", ...}            │
│  ]                                                          │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Code Agent 1: index.html                           │    │
│  │ Input:  file_info + contract                        │    │
│  │ Output: SSE event:code_file                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Code Agent 2: store.js                             │    │
│  │ Input:  file_info + contract                        │    │
│  │ Output: SSE event:code_file                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ... (并行执行)                                              │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Code Agent N: app.js                                │    │
│  │ Input:  file_info + contract                        │    │
│  │ Output: SSE event:code_file                        │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 每个 Code Agent 的处理逻辑

```
For each file in file_list:
    │
    ├──→ 提取 file_info:
    │       - file_id
    │       - file_path
    │       - purpose
    │       - depends_on
    │
    ├──→ 构建 prompt:
    │       - 当前文件职责 (purpose)
    │       - 全局接口契约 (contract)
    │       - 禁止使用 import/export
    │       - 使用全局 Vue 对象
    │       - 使用 Tailwind CSS
    │
    ├──→ 调用 LLM 生成代码
    │
    └──→ 输出:
            SSE event: code_file
            {
                file_id: "index",
                file_path: "index.html",
                code: "<!doctype html>..."
            }
```

### 输出

| SSE Event | 内容 |
|-----------|------|
| code_file (可多个) | `{file_id, file_path, code}` |

---

## Step 7: 完成

### 处理

```
┌─────────────────────────────────────────────────────────────┐
│  保存版本到数据库                                            │
│  - 为每个 code_file 保存到对应路径                          │
│  - 创建 AppVersionEntity                                     │
│                                                              │
│  更新应用状态                                                │
│  - 设置 current_version_id                                    │
└─────────────────────────────────────────────────────────────┘
```

### 输出

| SSE Event | 内容 |
|-----------|------|
| app_generated | `{uuid: "xxx", versionNumber: 1}` |
| done | `""` |

---

## 伪代码实现

### 主流程入口

```
class AppGenerationPipelineService {

    // Step 1: 入口方法
    Flux<ServerSentEvent<String>> execute(message, history, userId, sessionId, generateLogo) {

        // Step 2: 并行执行初始任务
        parallelInitialTasks = executeInitialTasksParallel(message, history);

        // Step 3: 等待 app_info 完成，并行执行后续任务
        parallelFollowUpTasks = parallelInitialTasks.appInfoMono
            .flatMapMany(appInfo -> executeFollowUpTasksParallel(appInfo, message));

        // Step 4: 契约设计
        contractTask = parallelFollowUpTasks.blueprintMono
            .flatMap(blueprint -> executeContractTask(blueprint));

        // Step 5: 文件生成
        codeTask = contractTask.contractMono
            .flatMapMany(contract -> executeCodeTask(blueprint, contract));

        // Step 6: 完成
        completeTask = codeTask
            .then(completeAppGeneration(appId, versionId));

        // 合并所有 SSE 流
        return merge(parallelInitialTasks, parallelFollowUpTasks,
                     contractTask, codeTask, completeTask);
    }
}
```

### Step 2: 并行执行初始任务

```
executeInitialTasksParallel(message, history) {

    // Agent A: Chat before Gen
    chatMono = ChatAgent.chatBeforeGen(message, history)
        .map(chunk -> SSE.event("message").data(chunk))
        .doOnNext(chatBuffer::append);

    // Agent B: 生成 Plan
    planMono = PlanGenerator.generatePlan(message)
        .map(chunk -> SSE.event("plan").data(chunk))
        .doOnNext(planBuffer::append);

    // Agent C: 生成应用信息
    appInfoMono = AppInfoExtractor.extract(message)
        .flatMap(info -> createAppEntity(userId, info))
        .doOnNext(app -> bindSessionToApp(sessionId, app.id))
        .map(app -> SSE.event("app_info").data(toJson(app.name, app.description, app.id)));

    // 并行执行
    return Flux.merge(chatMono, planMono, appInfoMono)
        .doOnComplete(() -> saveGenMessage(chatBuffer, planBuffer, appId));
}
```

### Step 4: 并行执行后续任务

```
executeFollowUpTasksParallel(appInfo, message) {

    // Task A: Logo 生成 (可选)
    logoFlux = generateLogo
        ? ImageService.generateLogoAndSave(app)
            .map(ext -> SSE.event("logo_generated").data(toJson(logoUuid, ext)))
        : Flux.empty();

    // Agent B: 架构设计
    blueprintFlux = ArchitectAgent.generateBlueprint(
            message,
            appInfo.name,
            appInfo.description
        )
        .reduce("", String::concat)
        .map(json -> SSE.event("blueprint").data(json))
        .doOnNext(blueprintBuffer::append);

    // 并行执行
    return Flux.merge(logoFlux, blueprintFlux)
        .cache();  // 缓存结果供后续使用
}
```

### Step 4: 契约设计

```
executeContractTask(blueprint) {
    blueprintJson = parseJson(blueprint);
    fileList = blueprintJson.projectBlueprint.fileList;

    return ContractAgent.generateContract(blueprint)
        .reduce("", String::concat)
        .map(contract -> SSE.event("contract").data(contract));
}
```

### Step 5: 文件生成

```
executeCodeTask(blueprint, contract) {
    fileList = blueprint.fileList;

    // 为每个文件创建生成任务
    codeFluxes = fileList.map(file ->
        CodeFileGenerator.generateCodeFile(
                toJson(file),  // file_id, file_path, purpose, depends_on
                contract
            )
            .reduce("", String::concat)
            .map(code -> SSE.event("code_file").data(toJson(file.fileId, file.filePath, code)))
    );

    // 并行执行所有文件生成
    return Flux.merge(codeFluxes.toArray());
}
```

### Step 6: 完成

```
completeAppGeneration(appId, versionId) {
    // 保存所有代码文件到存储
    saveCodeFiles(appId, versionId, codeFiles);

    // 创建版本记录
    version = appService.createVersion(appId, ..., "Initial version");

    // 返回完成事件
    return Flux.just(
        SSE.event("app_generated").data(toJson(app.uuid, version.number)),
        SSE.event("done").data("")
    );
}
```

---

## SSE 事件顺序

```
event:intent           ← Step 1
data:"generation"

event:message         ← Step 2 (并行)
data:"太好了！我这就帮你开发..."

event:plan            ← Step 2 (并行)
data:"- 梳理核心功能..."

event:app_info        ← Step 2 (结束)
data:{"name":"天气","description":"...","appId":123}

event:logo_generated  ← Step 3 (并行)
data:{"logoUuid":"xxx","ext":"png"}

event:blueprint       ← Step 3 (结束)
data:{"project_blueprint":{...}}

event:contract        ← Step 4
data:"# 全局接口契约..."

event:code_file       ← Step 5 (多个)
data:{"file_id":"index","file_path":"index.html","code":"..."}

event:code_file       ← Step 5 (多个)
data:{"file_id":"js_store","file_path":"store.js","code":"..."}

...

event:app_generated   ← Step 6
data:{"uuid":"abc-123","versionNumber":1}

event:done            ← Step 6
data:""
```

---

## 组件列表

| 组件 | 职责 | 输入 | 输出 |
|------|------|------|------|
| IntentAgent | 意图分类 | message | Enum: CHAT/GENERATION/EDIT |
| TemplateMatcherService | 模板匹配服务 | message | 模板名称或null |
| TemplateMatcherAgent | AI 模板匹配 | templatesList, message | 匹配结果 |
| TemplateFileService | 模板文件复制 | templateName, appId, version | 复制结果 |
| ChatAgent | 开场白 | message, history | Flux\<String\> |
| PlanGenerator | 实施计划 | message | Flux\<String\> |
| AppInfoExtractor | 应用信息 | message | AppInfoDTO |
| ImageService | Logo 生成 | app entity | logoUuid + ext |
| ArchitectAgent | 架构设计 | message, name, description | Blueprint JSON |
| ContractAgent | 契约设计 | blueprint | Markdown |
| CodeFileGenerator | 代码生成 | fileInfo, contract | code string |

---

## 提示词文件

| 文件 | 对应组件 | 说明 |
|------|----------|------|
| intent.txt | IntentAgent | 意图分类规则 |
| template-match.txt | TemplateMatcherAgent | 模板匹配规则 |
| gen-chat.txt | ChatAgent | 开场白 (100字) |
| gen-plan.txt | PlanGenerator | 实施计划 (4-8条) |
| gen-app-info.txt | AppInfoExtractor | 提取 name + description |
| gen-app-logo.txt | ImageService | Logo 生成提示词 |
| gen-architect.txt | ArchitectAgent | 项目蓝图生成 |
| gen-contract.txt | ContractAgent | 接口契约生成 |
| gen-code-file.txt | CodeFileGenerator | 单个代码文件生成 |