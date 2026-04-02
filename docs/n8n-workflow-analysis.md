# N8n 工作流分析文档

## 概述

本文档详细描述了 `My workflow 3.json` 中的 n8n 工作流架构，提取了所有使用的提示词（Prompts），并解释了每个节点的作用和数据流向。

---

## 工作流架构图

```
Webhook → AI Agent (意图分类) → Switch
                                      ├─→ ChatAgent
                                      ├─→ GenAgent → AI Agent1 (计划生成) → Code Split → AI Agent2 (代码生成)
                                      └─→ EditAgent
```

---

## 节点详解

### 1. Webhook (Webhook)

**作用**: 接收外部 HTTP POST 请求，触发整个工作流

**配置**:
- HTTP 方法: POST
- 路径: `a1efebd7-2b1a-4fea-b0fa-52617d55b4bf`
- 响应模式: lastNode

**输入**: `body.message` - 用户消息

---

### 2. AI Agent (意图分类)

**作用**: 分析用户输入，将其分类为三种意图之一

**模型**: qwen-plus (DashScope)

**系统提示词**:
```
Analyze the user's input and classify their intent into exactly one of the following three categories:
1. 'CHAT': The user wants to chat, ask questions, or discuss general topics.
2. 'GEN': The user wants to generate a webpage, an app, a tool, or code.
3. 'EDIT': The user wants to edit, refine, or modify an existing webpage, app, tool, or code.
Output ONLY the category name ('CHAT', 'GEN', or 'EDIT'). Do not include any punctuation, explanation, or extra text.
```

**输入**: `body.message`
**输出**: 意图分类结果 (CHAT | GEN | EDIT)

---

### 3. Switch (意图分流)

**作用**: 根据意图分类结果，将请求路由到不同的处理分支

**分支规则**:
- CHAT → ChatAgent
- GEN → GenAgent
- EDIT → EditAgent

---

### 4. ChatAgent (聊天模式)

**作用**: 处理聊天、问答类请求

**模型**: qwen-plus (DashScope)

**系统提示词**:
```
You are "YuanMeng" (元梦), an intelligent agent developed by "MetaCraft Workshop" (元创工坊).
You serve "MetaCraft" (元创空间) — an AI-native application generation and execution platform in the HarmonyOS ecosystem.
Project Creator: zeroicey.

Core Philosophy: Chat-to-App | Everyone is a Developer | Everything is Customizable.

Project Vision:
MetaCraft aims to eliminate professional barriers to software development, allowing users to create practical, persistent applications through natural language in seconds. It is part of the HarmonyOS ecosystem's long-tail application completion plan.

Key Features:
1. Creation Studio: Natural language programming, real-time preview, intelligent correction.
2. Meta Container: ArkWeb native-level rendering, Native Injection (JSBridge for hardware capabilities).
3. Universal Cloud: Data persistence, cross-device roaming.
4. Co-Market: One-click publishing, Remix (secondary creation).

Your Role:
Help users generate applications, plan features, or chat about the project.

Conversation Rules:
1. The incoming prompt may include a conversation history block from the same session.
2. Treat that history as prior context, but prioritize the user's current message when there is any ambiguity.
3. Do not repeat the full history unless the user explicitly asks for a recap.
```

---

### 5. GenAgent (生成模式)

**作用**: 接收应用生成需求，生成结构化的工程蓝图

**模型**: qwen-plus (DashScope)

**系统提示词**:
```
你是一位顶级的全栈软件架构师，隶属于"MetaCraft Workshop (元创工坊)"。
你的核心任务是接收用户的应用开发需求，并将其转化为一份结构化、零歧义、可被下游 AI 程序员严格执行的"工程蓝图"。

### 核心架构约束 (Hard Architecture Requirements):
1. **项目结构**：项目采用"无构建 (No-Build)"架构。只能包含 **唯一一个 `index.html`** 文件，以及 **多个 `.js`** 逻辑文件。禁止规划 `.vue`、`.ts` 或任何需要编译的文件。
2. **全局依赖引入**：`index.html` 必须且只能通过以下本地路径引入核心库，绝对禁止使用任何外部 CDN 链接：
   - Vue.js: `<script src='/public/js/vue.js'></script>`
   - Tailwind CSS: `<script src='/public/css/tailwindcss.js'></script>`
   - Bootstrap Icons: `<link rel='stylesheet' href='/public/css/bootstrap-icons.css'>`
3. **UI/组件规范**：
   - 禁用 DaisyUI 等第三方预设库。
   - 所有 UI 组件必须基于 Tailwind CSS 工具类从零构建。
   - 必须规划明确的视觉层级和 3 种基础状态（Loading 加载中、Empty 空数据、Error/Success 反馈）。
4. **JS 模块化原则**：虽然没有 Webpack/Vite，但必须对业务逻辑进行拆分。例如拆分为：`store.js` (全局状态)、`components.js` (Vue 组件注册)、`api.js` (模拟或真实数据请求)、`app.js` (Vue 实例初始化)。

### 输出格式要求（严格执行）：
你必须且只能输出一个合法的 JSON 对象，不要包含任何 Markdown 格式（不要使用 ```json 包裹），不要有任何解释性文本。JSON 结构必须完全匹配以下结构：

{
  "project_blueprint": {
    "app_name": "应用的英文缩写名称",
    "description": "应用的一句话描述",
    "global_state": "详细说明应用需要管理的核心数据状态（如：user_info, cart_items, loading_status）",
    "file_list": [
      {
        "file_id": "index",
        "file_path": "index.html",
        "file_type": "html",
        "purpose": "主 HTML 骨架。必须包含 Vue 挂载点 <div id='app'>，以及按照正确顺序引入所有的依赖库和下方的 js 文件。",
        "depends_on": ["所有 js 文件的 file_id"]
      },
      {
        "file_id": "js_store",
        "file_path": "store.js",
        "file_type": "javascript",
        "purpose": "定义全局响应式数据或状态管理逻辑 (如 Vue.reactive)",
        "depends_on": []
      },
      {
        "file_id": "js_app",
        "file_path": "app.js",
        "file_type": "javascript",
        "purpose": "实例化 Vue 并挂载到 #app，集成其他 js 模块的逻辑",
        "depends_on": ["js_store", "其他必要的 js_id"]
      }
    ]
  }
}
```

**输入**: 用户消息 (`body.message`)
**输出**: 结构化的 JSON 工程蓝图

---

### 6. EditAgent (编辑模式)

**作用**: 处理应用编辑、修改请求

**模型**: qwen-plus (DashScope)

**特点**: 使用默认系统提示词，无自定义配置

---

### 7. AI Agent1 (计划生成/接口契约)

**作用**: 为生成的蓝图定义全局接口契约

**模型**: qwen-plus (DashScope)

**系统提示词**:
```
你是一位资深的 API 契约设计师 (Interface Contractor)。
你接收到了架构师输出的应用蓝图。当前项目是一个"无构建 (No-Build)"的原生 HTML+JS 架构，多个 JS 文件通过 `<script>` 标签按顺序引入，这意味着它们只能通过**全局作用域**互相调用。

### 你的任务：
为你接收到的 `file_list` 中的每一个 JS 文件，严格定义它必须暴露到全局作用域的变量名和函数签名。
这个契约将作为后续并行生成代码时的"唯一真理 (Single Source of Truth)"。

### 契约规范：
1. **明确全局命名**：规定每个文件产出的核心变量/函数名（如 `const todoStore = ...`，`async function loadTasks() ...`）。
2. **明确参数与返回值**：必须写清楚函数的入参类型和返回值结构。
3. **避免冲突**：确保不同文件定义的全局变量不重名。

### 输出格式：
请输出一份清晰的 Markdown 格式的契约文档，不要包含具体的业务逻辑代码，只写接口定义。格式参考如下：

# 全局接口契约 (Global API Contract)

## 1. store.js
- 暴露全局常量: `const store = Vue.reactive({ ... })`
- 暴露全局函数: `function commit(action, payload) => void`

## 2. api.js
- 暴露全局函数: `async function loadTasks() => Array`
- 暴露全局函数: `async function saveTasks(tasks) => boolean`

【架构师蓝图输入】：
{blueprint.output}
```

**输入**: GenAgent 输出的蓝图 JSON
**输出**: Markdown 格式的接口契约文档

---

### 8. Code in JavaScript (数据拆分)

**作用**: 将蓝图的 file_list 拆分为多个 Item，每个文件都附带全局契约

**代码逻辑**:
```javascript
// 1. 获取阶段 A 的蓝图
const rawBlueprint = $('GenAgent').first().json.output;
const blueprint = JSON.parse(rawBlueprint).project_blueprint;

// 2. 获取阶段 B 的契约内容 (假设输出字段叫 output)
const contract = $input.first().json.output;

// 3. 将 file_list 拆分，并给每个文件附带上全局契约
return blueprint.file_list.map(file => {
  return {
    json: {
      ...file,
      app_name: blueprint.app_name,
      global_state: blueprint.global_state,
      global_contract: contract // 核心：每个 Item 都有这份契约了
    }
  };
});
```

**输出**: 多个并行的 Item，每个对应一个文件

---

### 9. AI Agent2 (代码生成)

**作用**: 根据每个文件的信息和全局契约，生成具体的代码

**模型**: qwen-coder-plus (DashScope)

**系统提示词**:
```
# 角色：元梦 (YuanMeng) - 元创工坊资深前端

你现在负责生成项目【{app_name}】中的一个具体文件。

## 你的参考资料：
1. **当前文件职责**：{purpose}
2. **全局接口契约（最高准则）**：
{global_contract}

## 必须遵守的硬性规定：
- **禁止使用 import/export**：这是一个纯 index.html 引入脚本的项目。
- **全局调用**：请根据【全局接口契约】中定义的变量名进行跨文件调用。
- **Vue 使用规范**：使用全局 Vue 对象，例如 `const { ref } = Vue;`。
- **UI 规范**：使用 Tailwind CSS 构建高质感组件，必须包含 Loading/Empty 状态。

## 输出要求：
只输出【{file_path}】的源代码，严禁包含 Markdown 块标记 (```) 或任何解释性文字。
```

**输入**: 包含 file_id, file_path, file_type, purpose, depends_on, app_name, global_state, global_contract 的 JSON
**输出**: 具体的源代码文件内容

---

## 数据流向

```
1. 用户发送 POST 请求到 Webhook
   └─> body.message: "创建一个天气预报应用"

2. AI Agent (意图分类)
   └─> 识别为 "GEN"

3. Switch 分流到 GenAgent

4. GenAgent (架构师)
   └─> 输出工程蓝图 JSON:
       {
         "project_blueprint": {
           "app_name": "weather",
           "file_list": [
             {"file_id": "index", "file_path": "index.html", ...},
             {"file_id": "js_store", "file_path": "store.js", ...},
             {"file_id": "js_app", "file_path": "app.js", ...}
           ]
         }
       }

5. AI Agent1 (契约设计师)
   └─> 输出接口契约 Markdown

6. Code in JavaScript
   └─> 拆分 file_list，为每个文件准备数据

7. AI Agent2 (并行执行 × 3)
   └─> index.html 源码
   └─> store.js 源码
   └─> app.js 源码
```

---

## 提示词汇总

### 英文提示词

| 节点 | 用途 | 语言 |
|------|------|------|
| AI Agent | 意图分类 | English |
| ChatAgent | 对话助手 | English |

### 中文提示词

| 节点 | 用途 | 语言 |
|------|------|------|
| GenAgent | 架构师生成蓝图 | 简体中文 |
| AI Agent1 | 接口契约设计 | 简体中文 |
| AI Agent2 | 代码生成 | 简体中文 |

---

## 技术要点

1. **No-Build 架构**: 纯 HTML + JS，无构建工具
2. **全局作用域通信**: 通过 `<script>` 标签顺序引入
3. **本地资源路径**:
   - `/public/js/vue.js`
   - `/public/css/tailwindcss.js`
   - `/public/css/bootstrap-icons.css`
4. **并行代码生成**: 使用 n8n 的循环功能并行生成多个文件
5. **强制契约约束**: 全局接口契约确保各文件间的兼容性

---

## 版本信息

- **工作流 ID**: UWkHziGbTbIHOgLO
- **版本 ID**: 70ef211d-2400-483a-b92d-1f76c4fcd2bd
- **n8n 节点版本**:
  - @n8n/n8n-nodes-langchain.agent: 3.1
  - n8n-nodes-base.switch: 3.4
  - n8n-nodes-base.code: 2

---

# 后端实现 (Spring Boot + LangChain4j)

> 本节描述当前生产环境中后端的 CHAT 模式实现，与 n8n 工作流的对应关系。

## 后端架构

```
POST /api/ai/agent/unified
    ↓
UnifiedOrchestrator (统一编排器)
    ↓
IntentAnalyzer (意图分类) → 返回 IntentDTO (CHAT | GEN | EDIT)
    ↓
    ├─→ CHAT  → ChatPipelineService → ChatAgent → SSE 流式输出
    ├─→ GEN  → AppGenPipelineService → 代码生成
    └─→ EDIT → AppEditPipelineService → 代码编辑
```

### 核心技术栈

| 技术 | 用途 |
|------|------|
| Spring Boot 3.5.9 | 框架 |
| LangChain4j 1.11.0-beta19 | AI 集成 |
| DashScope/Qwen 模型 | 大语言模型 |
| Reactor (Flux/Mono) | 响应式流式输出 |
| SSE (Server-Sent Events) | 实时推送 |

---

## CHAT 模式实现详解

### 1. UnifiedOrchestrator (统一编排器)

**文件**: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/UnifiedOrchestrator.java`

**职责**:
1. 接收 `AgentRequestDTO` 请求
2. 保存用户消息到数据库
3. 调用 `IntentAnalyzer` 进行意图分类
4. 根据意图分发到对应的 Pipeline Service

**核心代码**:
```java
public Flux<ServerSentEvent<String>> handleRequest(AgentRequestDTO request, Long userId) {
    return Mono.fromCallable(() -> prepareAndSaveUserMessage(request, userId))
            .flatMapMany(context -> Mono.fromCallable(() -> intentAnalyzer.analyze(context.message()))
                    .flatMapMany(intent -> {
                        log.info("User {} intent {}", userId, intent);

                        ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                                .event("intent")
                                .data(sseUtils.toIntentJson(intent.name()))
                                .build();

                        Flux<ServerSentEvent<String>> contentStream = switch (intent) {
                            case CHAT -> chatPipelineService.execute(context.message(), context.history(), userId,
                                    context.sessionId());
                            case GEN -> appGenPipelineService.execute(context.message(), context.history(), userId,
                                    context.sessionId(), context.generateLogo());
                            case EDIT -> appEditPipelineService.execute(context.message(), context.history(), userId,
                                context.sessionId());
                        };

                        return contentStream.startWith(intentEvent);
                    }))
            .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(sseUtils.toErrorJson(e.getMessage()))
                    .build()))
            .concatWithValues(doneEvent);
}
```

**SSE 事件类型**:
| Event | 描述 |
|-------|------|
| `intent` | 意图分类结果 (chat/gen/edit) |
| `message` | 流式 AI 响应内容 |
| `plan` | 应用生成计划 (GEN 模式) |
| `app_info` | 应用信息 (GEN 模式) |
| `logo_generated` | Logo 生成完成 (GEN 模式) |
| `app_generated` | 应用生成完成 (GEN 模式) |
| `done` | 流结束 |
| `error` | 错误信息 |

---

### 2. IntentAnalyzer (意图分类)

**文件**: `apps/api/src/main/java/com/metacraft/api/modules/ai/agent/IntentAnalyzer.java`

**接口定义**:
```java
@AiService
public interface IntentAnalyzer {
    @SystemMessage(fromResource = "prompts/intent.txt")
    @UserMessage("User request: {{it}}")
    IntentDTO analyze(String message);
}
```

**提示词** (`prompts/intent.txt`):
```
Analyze the user's input and classify their intent into exactly one of the following three categories:
1. 'CHAT': The user wants to chat, ask questions, or discuss general topics.
2. 'GEN': The user wants to generate a webpage, an app, a tool, or code.
3. 'EDIT': The user wants to edit, refine, or modify an existing webpage, app, tool, or code.
Output ONLY the category name ('CHAT', 'GEN', or 'EDIT'). Do not include any punctuation, explanation, or extra text.
```

**返回类型**: `IntentDTO` (枚举: CHAT, GEN, EDIT)

---

### 3. ChatPipelineService (聊天流水线)

**文件**: `apps/api/src/main/java/com/metacraft/api/modules/ai/service/pipeline/ChatPipelineService.java`

**职责**:
1. 调用 ChatAgent 获取流式响应
2. 将响应转换为 SSE 事件
3. 响应完成后保存到数据库

**核心代码**:
```java
public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
    StringBuilder assistantReply = new StringBuilder();

    return chatAgent.chat(message, history)
            .doOnNext(assistantReply::append)
            .doOnComplete(() -> saveAssistantMessage(userId, sessionId, assistantReply.toString()))
            .map(chunk -> ServerSentEvent.<String>builder()
                    .event("message")
                    .data(sseUtils.toContentJson(chunk))
                    .build());
}
```

**特点**:
- 返回 `Flux<ServerSentEvent<String>>` 实现流式输出
- 使用 `doOnNext` 收集完整响应
- 使用 `doOnComplete` 在响应完成后保存到数据库

---

### 4. ChatAgent (聊天 AI 服务)

**文件**: `apps/api/src/main/java/com/metacraft/api/modules/ai/agent/ChatAgent.java`

**接口定义**:
```java
@AiService
public interface ChatAgent {
    String NO_HISTORY = "No conversation history.";

    @SystemMessage(fromResource = "prompts/chat.txt")
    @UserMessage("""
            Conversation history:
            {{history}}

            Current user message:
            {{message}}
            """)
    Flux<String> chat(@V("message") String message, @V("history") String history);

    @SystemMessage(fromResource = "prompts/gen-chat.txt")
    @UserMessage("""
            Conversation history:
            {{history}}

            Current user message:
            {{message}}
            """)
    Flux<String> chatBeforeGen(@V("message") String message, @V("history") String history);

    @SystemMessage(fromResource = "prompts/edit-chat.txt")
    @UserMessage("""
            Conversation history:
            {{history}}

            Current user message:
            {{message}}
            """)
    Flux<String> chatBeforeEdit(@V("message") String message, @V("history") String history);
}
```

**提示词** (`prompts/chat.txt`):
```
You are "YuanMeng" (元梦), an intelligent agent developed by "MetaCraft Workshop" (元创工坊).
You serve "MetaCraft" (元创空间) — an AI-native application generation and execution platform in the HarmonyOS ecosystem.
Project Creator: zeroicey.

Core Philosophy: Chat-to-App | Everyone is a Developer | Everything is Customizable.

Project Vision:
MetaCraft aims to eliminate professional barriers to software development, allowing users to create practical, persistent applications through natural language in seconds. It is part of the HarmonyOS ecosystem's long-tail application completion plan.

Key Features:
1. Creation Studio: Natural language programming, real-time preview, intelligent correction.
2. Meta Container: ArkWeb native-level rendering, Native Injection (JSBridge for hardware capabilities).
3. Universal Cloud: Data persistence, cross-device roaming.
4. Co-Market: One-click publishing, Remix (secondary creation).

Your Role:
Help users generate applications, plan features, or chat about the project.

Conversation Rules:
1. The incoming prompt may include a conversation history block from the same session.
2. Treat that history as prior context, but prioritize the user's current message when there is any ambiguity.
3. Do not repeat the full history unless the user explicitly asks for a recap.
```

**返回值**: `Flux<String>` - 流式返回字符串片段

---

## 流式输出示例

### 请求
```bash
curl -X POST http://localhost:8080/api/ai/agent/unified \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下元创空间"}'
```

### 响应 (SSE 流)
```
event:intent
data:"chat"

event:message
data:"你好！欢迎来到元创空间..."

event:message
data！"元创空间 (MetaCraft) 是..."

event:done
data:""
```

---

## 后端 vs N8n 对比

| 功能 | N8n 工作流 | 后端实现 |
|------|------------|----------|
| 意图分类 | AI Agent (LangChain) | IntentAnalyzer (LangChain4j) |
| 聊天模式 | ChatAgent | ChatPipelineService + ChatAgent |
| 流式输出 | n8n 内置流式 | Flux\<ServerSentEvent\> |
| 代码生成 | GenAgent → AI Agent1 → AI Agent2 | AppGenPipelineService |
| 提示词管理 | 内嵌在节点配置中 | 外部 prompt 文件 (prompts/*.txt) |

---

## 提示词文件位置

所有后端提示词位于: `apps/api/src/main/resources/prompts/`

| 文件 | 用途 |
|------|------|
| `chat.txt` | CHAT 模式系统提示词 |
| `gen-chat.txt` | GEN 模式对话前缀 |
| `gen-plan.txt` | 生成计划提示词 |
| `gen-app-info.txt` | 提取应用信息提示词 |
| `gen-app-logo.txt` | 生成 Logo 提示词 |
| `gen-code.txt` | 生成代码提示词 |
| `edit-chat.txt` | EDIT 模式对话前缀 |
| `edit-code.txt` | 编辑代码提示词 |
| `intent.txt` | 意图分类提示词 |
| `template-match.txt` | 模板匹配提示词 |
| `gen-session-title.txt` | 会话标题生成提示词 |

---

# GEN 模式整合架构 (后端增强型)

> 本节描述将 N8n 工作流的架构理念整合到后端 GEN 模式的完整设计。

## 整合背景

### 原有后端 GEN 流程
```
1. ChatAgent.chatBeforeGen()     → 开场白 (100字温暖回复)
2. PlanGenerator                  → 4-8条实施计划 bullet
3. AppInfoExtractor               → 提取 name + description (JSON)
4. ImageService                   → Logo 生成 (Zhipu AI)
5. CodeGenerator                  → 生成 HTML + JS (2个文件)
```

### N8n GEN 流程
```
1. GenAgent (架构师)              → 生成项目蓝图 (JSON)
2. AI Agent1 (契约设计师)          → 生成接口契约 (Markdown)
3. Code Split                     → 拆分 file_list
4. AI Agent2 (并行)                → 生成多个 JS 文件
```

### 冲突点分析

| 方面 | 后端 | N8n | 问题 |
|------|------|-----|------|
| 应用信息 | AppInfoExtractor (name, description) | 集成在蓝图里 | 字段冲突 |
| 计划 | 独立步骤 gen-plan.txt | 集成在蓝图里 | 功能重叠 |
| 代码 | 2个大文件 (HTML+JS) | 多个模块化文件 | 架构不同 |

---

## 整合方案：增强型架构

### 核心原则

1. **Plan 和 ChatBeforeGen 并行** - 两者只给用户看，不影响后续 AI 处理
2. **AppInfoExtractor 先于 ArchitectAgent** - 避免 name/description 冲突
3. **Logo 提前到 AppInfoExtractor 后开始** - 不阻塞主流程
4. **引入 ArchitectAgent + ContractAgent** - 对齐 N8n 的模块化架构

### 完整流程

```
┌─────────────────────────────────────────────────────────────────┐
│  Phase 1: 并行执行 (Phase 1-2a)                                  │
│                                                              │
│   ┌──────────────────────┐        ┌──────────────────────┐   │
│   │ ChatAgent            │   ↔    │ PlanGenerator        │   │
│   │ chatBeforeGen()      │        │ generatePlan()       │   │
│   │ (开场白 100字)       │        │ (实施计划 4-8条)     │   │
│   └──────────┬───────────┘        └──────────┬───────────┘   │
│              ↓                               ↓               │
│       SSE: message                    SSE: plan              │
│                                                              │
│   谁先返回就先发，互不依赖，可并行                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Phase 2: 规划阶段 (串行)                                        │
│                                                              │
│   ┌──────────────────────────────────────────────────────────┐ │
│   │ AppInfoExtractor                                         │ │
│   │ extract(message)                                         │ │
│   │ 输出: {"name":"天气", "description":"..."}              │ │
│   │ 同时创建 AppEntity 到数据库                               │ │
│   └─────────────────────────┬────────────────────────────────┘ │
│                             ↓                                  │
│                      SSE: app_info                            │
│                             ↓                                  │
│   ┌──────────────────────────────────────────────────────────┐ │
│   │ Logo 生成 (独立执行，不阻塞主流程)                        │ │
│   │ ImageService.generateLogoAndSave()                        │ │
│   └─────────────────────────┬────────────────────────────────┘ │
│                             ↓                                  │
│                      SSE: logo_generated                       │
│                             ↓                                  │
│   ┌──────────────────────────────────────────────────────────┐ │
│   │ ArchitectAgent (架构师)                                   │ │
│   │ generateBlueprint(message, name, description)              │ │
│   │ 使用已确认的 name/description 生成项目蓝图                 │ │
│   │ 输出: project_blueprint { app_name, description, file_list }│ │
│   └─────────────────────────┬────────────────────────────────┘ │
│                             ↓                                  │
│                      SSE: blueprint                            │
│                             ↓                                  │
│   ┌──────────────────────────────────────────────────────────┐ │
│   │ ContractAgent (契约设计师)                                │ │
│   │ generateContract(blueprint)                               │ │
│   │ 为 file_list 中每个文件定义接口契约                       │ │
│   │ 输出: Markdown 格式的全局接口契约                          │ │
│   └─────────────────────────┬────────────────────────────────┘ │
│                             ↓                                  │
│                      SSE: contract                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Phase 3: 代码生成 (基于 file_list + contract)                  │
│                                                              │
│   ┌──────────────────────────────────────────────────────────┐ │
│   │ CodeFileGenerator (文件生成器)                            │ │
│   │ 为 file_list 中每个 file_id 生成对应代码                 │ │
│   │                                                          │ │
│   │   index.html  ←  入口文件，依赖所有 js                   │ │
│   │   store.js    ←  Vue.reactive 状态管理                   │ │
│   │   api.js      ←  数据请求模块                            │ │
│   │   app.js      ←  Vue 实例和组件注册                     │ │
│   │   ...        ←  其他模块文件                             │ │
│   └─────────────────────────┬────────────────────────────────┘ │
│                             ↓                                  │
│   SSE: code_file (可多个，按 file_id 逐个发送)                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Phase 4: 完成                                                  │
│                                                              │
│   SSE: app_generated → done                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 依赖关系图

```
         chatBeforeGen ─────────┐
                                ├──→ 并行 → SSE: message + plan
         planGenerator ─────────┘

                                    ┌─→ app_info
                                    │
         appInfoExtractor ──────────┼─→ logo_generated (独立)
                                    │
                                    └─→ blueprint (使用 name)
         architectAgent (依赖name) ─┘
                                    │
                                    └─→ contract (依赖 file_list)
         contractAgent ────────────┼─→ code_file (可多个)
                                    │
         codeFileGenerator ────────┘
                                    │
                                    └─→ app_generated → done
```

### 各阶段详细说明

#### Phase 1-2a: 并行阶段

| 组件 | 输入 | 输出 | 依赖 |
|------|------|------|------|
| ChatAgent.chatBeforeGen() | message, history | SSE: message | 无 |
| PlanGenerator | message | SSE: plan | 无 |

**说明**: 两个组件完全独立，返回即发送，谁先完成先发哪个。

#### Phase 2: 串行规划阶段

| 组件 | 输入 | 输出 | 依赖 |
|------|------|------|------|
| AppInfoExtractor | message | AppInfoDTO {name, description} | 无 |
| Logo 生成 | app entity | SSE: logo_generated | appInfoExtractor |
| ArchitectAgent | message, name, description | project_blueprint | appInfoExtractor |
| ContractAgent | blueprint | contract (Markdown) | architectAgent |

**说明**:
- AppInfoExtractor 优先执行，产出确定的 name/description
- Logo 生成独立执行，不阻塞后续流程
- ArchitectAgent 使用确认的 name/description，避免冲突

#### Phase 3: 代码生成阶段

| 组件 | 输入 | 输出 | 依赖 |
|------|------|------|------|
| CodeFileGenerator | file_list, contract | SSE: code_file (多个) | ContractAgent |

---

## 新增组件设计

### 1. ArchitectAgent (架构师)

类似 N8n 的 GenAgent，生成结构化蓝图。

**接口定义**:
```java
@AiService
public interface ArchitectAgent {
    @SystemMessage(fromResource = "prompts/gen-architect.txt")
    @UserMessage("""
            User requirement: {{message}}
            App name: {{name}}
            App description: {{description}}
            """)
    Flux<String> generateBlueprint(@V("message") String message,
                                     @V("name") String name,
                                     @V("description") String description);
}
```

**提示词** (`prompts/gen-architect.txt`):
```
你是一位顶级的全栈软件架构师，隶属于"MetaCraft Workshop (元创工坊)"。
你的核心任务是接收用户的应用开发需求，并将其转化为一份结构化、零歧义、可被下游 AI 程序员严格执行的"工程蓝图"。

### 核心架构约束:
1. 项目结构：No-Build 架构，唯一 index.html + 多个 .js 文件
2. 依赖引入：/public/js/vue.js, /public/css/tailwind.js, /public/css/bootstrap-icons.css
3. UI规范：禁用 DaisyUI，基于 Tailwind CSS 从零构建
4. 模块化：store.js, api.js, app.js 等拆分

### 输出格式 (JSON):
{
  "project_blueprint": {
    "app_name": "应用英文缩写",
    "description": "一句话描述",
    "file_list": [
      {"file_id": "index", "file_path": "index.html", "purpose": "...", "depends_on": ["js_store", "js_app"]},
      {"file_id": "js_store", "file_path": "store.js", "purpose": "...", "depends_on": []},
      {"file_id": "js_api", "file_path": "api.js", "purpose": "...", "depends_on": []},
      {"file_id": "js_app", "file_path": "app.js", "purpose": "...", "depends_on": ["js_store", "js_api"]}
    ]
  }
}
```

### 2. ContractAgent (契约设计师)

类似 N8n 的 AI Agent1，为每个文件定义接口契约。

**接口定义**:
```java
@AiService
public interface ContractAgent {
    @SystemMessage(fromResource = "prompts/gen-contract.txt")
    @UserMessage("{{blueprint}}")
    Flux<String> generateContract(String blueprint);
}
```

**提示词** (`prompts/gen-contract.txt`):
```
你是一位资深的 API 契约设计师 (Interface Contractor)。
你接收到了架构师输出的应用蓝图。当前项目是一个"无构建 (No-Build)"的原生 HTML+JS 架构。

### 你的任务：
为 file_list 中的每一个 JS 文件，严格定义它必须暴露到全局作用域的变量名和函数签名。

### 契约规范：
1. 明确全局命名：规定每个文件的核心变量/函数名
2. 明确参数与返回值：写清楚函数入参类型和返回值结构
3. 避免冲突：确保不同文件定义的全局变量不重名

### 输出格式：
请输出 Markdown 格式的契约文档。

【架构师蓝图输入】：
{{blueprint}}
```

### 3. CodeFileGenerator (代码文件生成器)

根据 file_id 和 contract 生成具体代码。

**接口定义**:
```java
@AiService
public interface CodeFileGenerator {
    @SystemMessage(fromResource = "prompts/gen-code-file.txt")
    @UserMessage("""
            File info: {{fileInfo}}
            Global contract: {{contract}}
            """)
    Flux<String> generateCodeFile(@V("fileInfo") String fileInfo,
                                    @V("contract") String contract);
}
```

---

## SSE 事件完整列表

| Event | Phase | 描述 | 数据格式 |
|-------|-------|------|----------|
| intent | 0 | 意图分类 | `"gen"` |
| message | 1 | 开场白 | JSON string |
| plan | 1 | 实施计划 | JSON string |
| app_info | 2a | 应用名称+描述 | `{name, description, appId}` |
| logo_generated | 2b | Logo 生成 | `{logoUuid, ext}` |
| blueprint | 2c | 项目蓝图 | `{project_blueprint}` |
| contract | 2d | 接口契约 | Markdown string |
| code_file | 3 | 代码文件 (多个) | `{fileId, filePath, code}` |
| app_generated | 4 | 完成 | `{uuid, versionNumber}` |
| done | - | 流结束 | `""` |
| error | - | 错误信息 | `{message}` |

---

## 后端 vs N8n 对比 (更新)

| 功能 | N8n 工作流 | 后端实现 (增强型) |
|------|------------|------------------|
| 意图分类 | AI Agent | IntentAnalyzer |
| 开场白 | - | ChatAgent.chatBeforeGen |
| 实施计划 | - | PlanGenerator (并行) |
| 应用信息 | 集成在蓝图 | AppInfoExtractor (独立) |
| Logo 生成 | - | ImageService (提前) |
| 架构师 | GenAgent | ArchitectAgent |
| 契约设计 | AI Agent1 | ContractAgent |
| 代码生成 | AI Agent2 (并行) | CodeFileGenerator (串行) |
| 输出文件 | 多个 | 多个 (按 file_list) |

---

## 提示词文件 (更新)

新增提示词:

| 文件 | 用途 |
|------|------|
| `gen-architect.txt` | 架构师生成项目蓝图 |
| `gen-contract.txt` | 契约设计师生成接口契约 |
| `gen-code-file.txt` | 代码文件生成器 |