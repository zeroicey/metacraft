# 元创页面 SSE 事件处理实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 web 端的元创页面实现实时流式 SSE 事件处理，参照鸿蒙端实时展示应用生成过程

**Architecture:** 修改 useChatStream hook 添加新事件回调，创建 AI 元素组件（PlanCard、AppInfoCard、AppPreviewCard），更新 yuanchuang.tsx 实时展示生成过程

**Tech Stack:** React, TypeScript, Streamdown, Tailwind CSS

---

## 文件结构

```
apps/web/src/
├── hooks/
│   └── useChatStream.ts          # 修改：添加新事件回调
├── components/ai-elements/
│   ├── index.ts                  # 新建：导出所有组件
│   ├── plan-card.tsx            # 新建：计划卡片
│   ├── app-info-card.tsx        # 新建：应用信息卡片
│   ├── app-preview-card.tsx     # 新建：应用预览卡片
│   └── gen-message-card.tsx     # 新建：生成消息卡片（组合组件）
└── pages/
    └── yuanchuang.tsx           # 修改：添加状态管理和渲染逻辑
```

---

## Chunk 1: 增强 useChatStream Hook

### Task 1: 扩展 useChatStream 事件回调

**Files:**
- Modify: `apps/web/src/hooks/useChatStream.ts`

- [ ] **Step 1: 添加新的类型定义**

在 `useChatStream.ts` 文件顶部添加：

```typescript
/** 应用信息 */
export interface AppInfo {
  name: string;
  description: string;
}

/** Logo 生成数据 */
export interface LogoData {
  uuid: string;
  ext: string;
}

/** 应用生成数据 */
export interface AppGeneratedData {
  uuid: string;
  version: number;
}
```

- [ ] **Step 2: 扩展 SendMessageOptions 接口**

修改现有的 `SendMessageOptions` 接口，添加新回调：

```typescript
export interface SendMessageOptions {
  message: string;
  sessionId: string;
  onMessage?: (content: string) => void;
  onIntent?: (intent: SSEIntent) => void;
  onPlan?: (plan: string) => void;
  onAppInfo?: (info: AppInfo) => void;
  onLogoGenerated?: (data: LogoData) => void;
  onAppGenerated?: (data: AppGeneratedData) => void;
  onDone?: () => void;
  onError?: (error: string) => void;
}
```

- [ ] **Step 3: 添加事件处理逻辑**

在 `sendMessage` 函数的 SSE 事件循环中，添加对 `plan`、`app_info`、`logo_generated`、`app_generated` 事件的处理：

在现有的 `eventType === "message"` 之后添加：

```typescript
} else if (eventType === "plan") {
  const data = event.data;
  try {
    const parsed = JSON.parse(data);
    const plan = parsed.plan as string;
    if (plan) {
      onPlan?.(plan);
    }
  } catch {
    if (data) {
      onPlan?.(data);
    }
  }
} else if (eventType === "app_info") {
  const data = event.data;
  try {
    const parsed = JSON.parse(data);
    const info: AppInfo = {
      name: parsed.name as string,
      description: parsed.description as string,
    };
    onAppInfo?.(info);
  } catch (e) {
    console.error("Failed to parse app_info:", e);
  }
} else if (eventType === "logo_generated") {
  const data = event.data;
  try {
    const parsed = JSON.parse(data);
    const logoData: LogoData = {
      uuid: parsed.uuid as string,
      ext: parsed.ext as string,
    };
    onLogoGenerated?.(logoData);
  } catch (e) {
    console.error("Failed to parse logo_generated:", e);
  }
} else if (eventType === "app_generated") {
  const data = event.data;
  try {
    const parsed = JSON.parse(data);
    const appData: AppGeneratedData = {
      uuid: parsed.uuid as string,
      version: parsed.version as number,
    };
    onAppGenerated?.(appData);
  } catch (e) {
    console.error("Failed to parse app_generated:", e);
  }
}
```

- [ ] **Step 4: 运行类型检查**

```bash
cd /home/oicey/projects/metacraft/apps/web && npx tsc --noEmit
```

Expected: 无错误

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/hooks/useChatStream.ts
git commit -m "feat(web): add new SSE event callbacks to useChatStream

- Add AppInfo, LogoData, AppGeneratedData types
- Extend SendMessageOptions with new callbacks
- Handle plan, app_info, logo_generated, app_generated events

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 2: 创建 AI 元素组件

### Task 2: 创建 PlanCard 组件

**Files:**
- Create: `apps/web/src/components/ai-elements/plan-card.tsx`

- [ ] **Step 1: 创建 PlanCard 组件**

```typescript
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";

interface PlanCardProps {
  plan: string;
}

export function PlanCard({ plan }: PlanCardProps) {
  return (
    <div className="mt-2 rounded-xl border border-[#D9E5FF] bg-[#F6F9FF] p-3">
      <div className="mb-2 flex items-center justify-between">
        <span className="text-sm font-bold text-[#2F5DFF]">应用计划</span>
        <span className="text-xs text-[#7C8AA5]">PLAN</span>
      </div>
      {plan ? (
        <div className="max-h-[220px] overflow-y-auto">
          <Streamdown
            plugins={{
              code,
              mermaid,
              math,
              cjk,
            }}
          >
            {plan}
          </Streamdown>
        </div>
      ) : (
        <span className="text-xs text-[#8E8E93]">计划生成中...</span>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/ai-elements/plan-card.tsx
git commit -m "feat(web): add PlanCard component for displaying app generation plan

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

### Task 3: 创建 AppInfoCard 组件

**Files:**
- Create: `apps/web/src/components/ai-elements/app-info-card.tsx`

- [ ] **Step 1: 创建 AppInfoCard 组件**

```typescript
import { ExternalLinkIcon } from "lucide-react";

interface AppInfoCardProps {
  appName?: string;
  appDescription?: string;
  logoUrl?: string;
}

export function AppInfoCard({
  appName,
  appDescription,
  logoUrl,
}: AppInfoCardProps) {
  const hasContent =
    (appName && appName.length > 0) ||
    (appDescription && appDescription.length > 0) ||
    (logoUrl && logoUrl.length > 0);

  if (!hasContent) {
    return null;
  }

  return (
    <div className="mt-2 rounded-xl border border-[#E5E5E5] bg-white p-4">
      <div className="flex items-start gap-3">
        {logoUrl ? (
          <img
            src={logoUrl}
            alt={appName || "App Logo"}
            className="h-12 w-12 rounded-lg object-cover"
          />
        ) : (
          <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-gradient-to-br from-[#667eea] to-[#764ba2]">
            <span className="text-xl font-bold text-white">
              {appName?.charAt(0) || "A"}
            </span>
          </div>
        )}
        <div className="flex-1">
          <h3 className="font-semibold text-gray-800">{appName || "未命名应用"}</h3>
          {appDescription && (
            <p className="mt-1 text-sm text-gray-500">{appDescription}</p>
          )}
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/ai-elements/app-info-card.tsx
git commit -m "feat(web): add AppInfoCard component for displaying app info

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

### Task 4: 创建 AppPreviewCard 组件

**Files:**
- Create: `apps/web/src/components/ai-elements/app-preview-card.tsx`

- [ ] **Step 1: 创建 AppPreviewCard 组件**

```typescript
import { ExternalLinkIcon, Maximize2Icon } from "lucide-react";
import { useState } from "react";

interface AppPreviewCardProps {
  previewUrl: string;
  appName?: string;
  logoUrl?: string;
}

export function AppPreviewCard({
  previewUrl,
  appName,
  logoUrl,
}: AppPreviewCardProps) {
  const [isLoading, setIsLoading] = useState(true);
  const resolvedUrl = previewUrl.startsWith("/")
    ? `http://localhost:8080${previewUrl}`
    : previewUrl;

  const handleOpenNewWindow = () => {
    window.open(resolvedUrl, "_blank");
  };

  return (
    <div className="mt-2 rounded-xl border border-[#EEEEEE] bg-[#F5F7FA] p-2.5">
      <div className="mb-2.5 flex items-center justify-end">
        <button
          onClick={handleOpenNewWindow}
          className="flex h-7 w-7 items-center justify-center rounded-full bg-[#007AFF] text-white transition hover:bg-[#0056CC]"
          title="在新窗口打开"
        >
          <Maximize2Icon className="h-4 w-4" />
        </button>
      </div>
      {resolvedUrl ? (
        <div className="relative h-[210px] overflow-hidden rounded-lg bg-white">
          <iframe
            src={resolvedUrl}
            className="h-full w-full border-0"
            title={`${appName || "App"} Preview`}
            onLoad={() => setIsLoading(false)}
            sandbox="allow-scripts allow-same-origin allow-forms"
          />
          {isLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white">
              <span className="text-sm text-gray-400">预览加载中...</span>
            </div>
          )}
        </div>
      ) : (
        <div className="flex h-[210px] items-center justify-center rounded-lg bg-white">
          <span className="text-sm text-gray-400">预览加载中...</span>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/ai-elements/app-preview-card.tsx
git commit -m "feat(web): add AppPreviewCard component for embedding app preview

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

### Task 5: 创建 GenMessageCard 组合组件

**Files:**
- Create: `apps/web/src/components/ai-elements/gen-message-card.tsx`

- [ ] **Step 1: 创建 GenMessageCard 组件**

```typescript
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import { PlanCard } from "./plan-card";
import { AppInfoCard } from "./app-info-card";
import { AppPreviewCard } from "./app-preview-card";

interface GenMessageCardProps {
  chatBeforeGen?: string;
  plan?: string;
  appName?: string;
  appDescription?: string;
  logoUrl?: string;
  previewUrl?: string;
  isStreaming?: boolean;
}

export function GenMessageCard({
  chatBeforeGen,
  plan,
  appName,
  appDescription,
  logoUrl,
  previewUrl,
  isStreaming = false,
}: GenMessageCardProps) {
  return (
    <div className="flex flex-col gap-2">
      {/* 聊天内容 */}
      {chatBeforeGen && chatBeforeGen.length > 0 && (
        <div className="text-sm text-gray-700">
          <Streamdown
            plugins={{
              code,
              mermaid,
              math,
              cjk,
            }}
            isAnimating={isStreaming}
          >
            {chatBeforeGen}
          </Streamdown>
        </div>
      )}

      {/* 计划 */}
      {plan && plan.length > 0 && <PlanCard plan={plan} />}

      {/* 应用信息 */}
      {(appName || appDescription || logoUrl) && (
        <AppInfoCard
          appName={appName}
          appDescription={appDescription}
          logoUrl={logoUrl}
        />
      )}

      {/* 预览 */}
      {previewUrl && (
        <AppPreviewCard
          previewUrl={previewUrl}
          appName={appName}
          logoUrl={logoUrl}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 2: 创建 index.ts 导出文件**

```typescript
export { PlanCard } from "./plan-card";
export { AppInfoCard } from "./app-info-card";
export { AppPreviewCard } from "./app-preview-card";
export { GenMessageCard } from "./gen-message-card";
```

- [ ] **Step 3: Commit**

```bash
git add apps/web/src/components/ai-elements/
git commit -m "feat(web): add GenMessageCard and index exports

- Add GenMessageCard combining all gen-related components
- Add index.ts with exports for all ai-elements

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Chunk 3: 更新 YuanChuang 页面

### Task 6: 集成事件处理到 yuanchuang.tsx

**Files:**
- Modify: `apps/web/src/pages/yuanchuang.tsx`

- [ ] **Step 1: 添加状态管理**

在现有状态后添加：

```typescript
// SSE 事件相关状态
const [currentIntent, setCurrentIntent] = useState<SSEIntent | null>(null);
const [planContent, setPlanContent] = useState("");
const [appInfo, setAppInfo] = useState<{ name: string; description: string } | null>(null);
const [logoData, setLogoData] = useState<{ uuid: string; ext: string } | null>(null);
const [appGeneratedData, setAppGeneratedData] = useState<{ uuid: string; version: number } | null>(null);
```

- [ ] **Step 2: 计算 Logo 和 Preview URL**

在组件中添加辅助函数：

```typescript
// 计算 Logo URL
const logoUrl = logoData
  ? `http://localhost:8080/api/apps/logo/${logoData.uuid}.${logoData.ext}`
  : undefined;

// 计算 Preview URL
const previewUrl = appGeneratedData
  ? `/api/preview/${appGeneratedData.uuid}`
  : undefined;
```

- [ ] **Step 3: 修改 sendMessage 调用**

更新现有的 `sendMessage` 调用，添加新回调：

```typescript
sendMessage({
  message: messageText,
  sessionId: selectedSessionId,
  onMessage: (content) => {
    setStreamingContent((prev) => prev + content);
  },
  onIntent: (intent) => {
    setCurrentIntent(intent);
    console.log("Intent:", intent);
  },
  onPlan: (plan) => {
    setPlanContent((prev) => prev + plan);
  },
  onAppInfo: (info) => {
    setAppInfo(info);
  },
  onLogoGenerated: (data) => {
    setLogoData(data);
  },
  onAppGenerated: (data) => {
    setAppGeneratedData(data);
  },
  onDone: async () => {
    setStreamingContent("");
    setCurrentIntent(null);
    setPlanContent("");
    setAppInfo(null);
    setLogoData(null);
    setAppGeneratedData(null);
    setLocalMessages([]);
    await refetch();
  },
  onError: (errorMsg) => {
    toast.error(errorMsg);
    setLocalMessages((prev) => prev.filter((m) => m.id !== tempUserMessage.id));
  },
});
```

- [ ] **Step 4: 添加 GenMessageCard 渲染**

在消息列表的 assistant 消息渲染部分，添加条件渲染：

```typescript
{/* 流式输出的内容 */}
{streamingContent && (
  <div>
    {/* 意图指示器 */}
    {currentIntent && (
      <div className="mb-2 flex items-center gap-2">
        <span className="rounded-full bg-[#2F5DFF] px-2 py-0.5 text-xs text-white">
          {currentIntent === "gen" ? "生成应用" : currentIntent === "edit" ? "编辑应用" : "对话"}
        </span>
      </div>
    )}

    {/* 生成模式：显示 GenMessageCard */}
    {currentIntent === "gen" || currentIntent === "edit" ? (
      <GenMessageCard
        chatBeforeGen={streamingContent}
        plan={planContent}
        appName={appInfo?.name}
        appDescription={appInfo?.description}
        logoUrl={logoUrl}
        previewUrl={previewUrl}
        isStreaming={isStreaming}
      />
    ) : (
      /* 对话模式：显示普通消息 */
      <Streamdown
        animated={{
          animation: "blurIn",
          duration: 200,
          easing: "ease-out",
          sep: "word",
        }}
        plugins={{
          code,
          mermaid,
          math,
          cjk,
        }}
        isAnimating={isStreaming}
      >
        {streamingContent}
      </Streamdown>
    )}
  </div>
)}
```

- [ ] **Step 5: 清除会话切换时的状态**

确保在 `useEffect` 中清除所有 SSE 状态：

```typescript
useEffect(() => {
  setLocalMessages([]);
  setStreamingContent("");
  setCurrentIntent(null);
  setPlanContent("");
  setAppInfo(null);
  setLogoData(null);
  setAppGeneratedData(null);
}, [selectedSessionId]);
```

- [ ] **Step 6: 添加导入**

```typescript
import { GenMessageCard } from "@/components/ai-elements/gen-message-card";
import type { AppInfo, LogoData, AppGeneratedData } from "@/hooks/useChatStream";
```

- [ ] **Step 7: 运行类型检查**

```bash
cd /home/oicey/projects/metacraft/apps/web && npx tsc --noEmit
```

Expected: 无错误

- [ ] **Step 8: Commit**

```bash
git add apps/web/src/pages/yuanchuang.tsx
git commit -m "feat(web): integrate SSE events into yuanchuang page

- Add state for plan, appInfo, logoData, appGeneratedData
- Add GenMessageCard rendering for gen/edit intents
- Clear SSE state on session change

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验证步骤

- [ ] 启动后端 `cd apps/api && ./mvnw spring-boot:run`
- [ ] 启动前端 `cd apps/web && npm run dev`
- [ ] 登录用户
- [ ] 在元创页面点击"创建新应用"
- [ ] 发送"帮我创建一个计算器应用"
- [ ] 验证事件流：
  - [ ] 显示 intent 指示器
  - [ ] 实时显示 plan 内容
  - [ ] 显示 app_info (名称、描述)
  - [ ] 显示 logo
  - [ ] 显示 app_generated (预览)
- [ ] 确保无 TypeScript 错误