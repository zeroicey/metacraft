# 元创页面聊天功能重新实现划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 yuanchuang 页面消息处理，移除 TanStack Query，参考 Huawei 实现 SSE 流式显示

**Architecture:** 使用 useState + 自定义 hook，不依赖第三方状态管理库。API 层直接用 ky，SSE 解析参考 Huawei 的 SSEClient.ets（按 \n\n 分割事件）

**Tech Stack:** React, ky, useState, useRef, Streamdown

---

## File Structure

```
apps/web/src/
├── api/
│   └── chat.ts                    # 新建：聊天 API 函数（获取消息、发送消息）
├── hooks/
│   └── useChat.ts                 # 新建：核心 hook（消息列表 + SSE 流式）
├── pages/
│   └── yuanchuang.tsx             # 修改：简化为调用 useChat
├── components/
│   └── ai-elements/              # 已存在：GenMessageCard 等组件
└── lib/
    └── http.ts                    # 已存在：ky HTTP 客户端
```

---

## Chunk 1: API 层

### Task 1: 创建 chat.ts API 模块

**Files:**
- Create: `apps/web/src/api/chat.ts`

- [ ] **Step 1: 创建文件结构**

```typescript
import http, { type ApiResponse } from "@/lib/http";

/**
 * 获取会话消息
 * @param sessionId 会话 ID
 */
export const getMessages = async (sessionId: string) => {
    const response = await http
        .get(`ai/sessions/${sessionId}/messages`)
        .json<ApiResponse<any[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch messages");
    }
    return response.data;
};

/**
 * SSE 流式发送消息
 * @param message 消息内容
 * @param sessionId 会话 ID
 * @param signal AbortSignal
 */
export const sendMessageStream = async (
    message: string,
    sessionId: string,
    signal?: AbortSignal
): Promise<Response> => {
    return await http.post("ai/agent/unified", {
        json: { message, sessionId },
        headers: { Accept: "text/event-stream" },
        signal,
        stream: true,
    });
};
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/api/chat.ts
git commit -m "feat(web): add chat API functions"
```

---

## Chunk 2: Hook 层

### Task 2: 创建 useChat hook

**Files:**
- Create: `apps/web/src/hooks/useChat.ts`

- [ ] **Step 1: 定义类型**

```typescript
import { useState, useCallback, useRef, useEffect } from "react";
import { getMessages, sendMessageStream } from "@/api/chat";

/** SSE 事件类型 */
export type SSEIntent = "chat" | "gen" | "edit";

/** 聊天消息 */
export interface ChatMessage {
    id: number;
    sessionId: string;
    role: "user" | "assistant";
    content: string;
    createdAt: string;
    // 流式过程动态更新的字段
    intent?: SSEIntent;
    plan?: string;
    appName?: string;
    appDescription?: string;
    logoUrl?: string;
    previewUrl?: string;
    isStreaming?: boolean;
    error?: string;
}
```

- [ ] **Step 2: 实现 useChat hook 主体**

```typescript
interface UseChatReturn {
    messages: ChatMessage[];
    isLoading: boolean;
    isStreaming: boolean;
    error: string | null;
    sendMessage: (content: string) => Promise<void>;
    loadHistory: (sessionId: string) => Promise<void>;
    cancelStream: () => void;
}

export function useChat(sessionId: string): UseChatReturn {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isStreaming, setIsStreaming] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const abortControllerRef = useRef<AbortController | null>(null);
    const streamingMessageIdRef = useRef<number | null>(null);

    // 加载历史消息
    const loadHistory = useCallback(async (sid: string) => {
        if (!sid) return;
        setIsLoading(true);
        setError(null);
        try {
            const data = await getMessages(sid);
            setMessages(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load messages");
        } finally {
            setIsLoading(false);
        }
    }, []);

    // 会话切换时加载
    useEffect(() => {
        if (sessionId) {
            loadHistory(sessionId);
        } else {
            setMessages([]);
        }
    }, [sessionId, loadHistory]);
```

- [ ] **Step 3: 实现 SSE 解析逻辑（参考 Huawei）**

```typescript
    // SSE 解析（按 \n\n 分割）
    const parseSSE = useCallback(async (response: Response) => {
        const reader = response.body?.getReader();
        if (!reader) throw new Error("No reader");

        const decoder = new TextDecoder("utf-8");
        let buffer = "";

        try {
            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    if (buffer.trim()) {
                        // 处理最后的数据
                    }
                    break;
                }

                buffer += decoder.decode(value, { stream: true });

                // 按 \n\n 分割（标准 SSE 格式）
                const events = buffer.split("\n\n");
                buffer = events.pop() || "";

                for (const eventBlock of events) {
                    if (!eventBlock.trim()) continue;

                    const lines = eventBlock.split("\n");
                    let eventName = "";
                    let eventData = "";

                    for (const line of lines) {
                        const trimmed = line.trimStart();
                        if (trimmed.startsWith("event:")) {
                            eventName = trimmed.substring(6).trim();
                        } else if (trimmed.startsWith("data:")) {
                            let content = trimmed.substring(5);
                            if (content.startsWith(" ")) content = content.substring(1);
                            eventData = content;
                        }
                    }

                    // 根据事件类型处理
                    handleEvent(eventName, eventData);
                }
            }
        } finally {
            reader.releaseLock();
        }
    }, []);

    // 处理单个事件
    const handleEvent = useCallback((eventName: string, eventData: string) => {
        let parsed: Record<string, unknown> = {};
        try {
            parsed = JSON.parse(eventData);
        } catch {
            // 非 JSON
        }

        switch (eventName) {
            case "intent": {
                const intent = (parsed.intent as string) || eventData;
                if (intent && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, intent: intent as SSEIntent }
                                : m
                        )
                    );
                }
                break;
            }
            case "message": {
                const content = (parsed.content as string) || eventData;
                if (content && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, content: m.content + content }
                                : m
                        )
                    );
                }
                break;
            }
            case "plan": {
                const plan = (parsed.plan as string) || eventData;
                if (plan && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, plan }
                                : m
                        )
                    );
                }
                break;
            }
            case "app_info": {
                const name = (parsed.name as string) || "";
                const description = (parsed.description as string) || "";
                if (streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, appName: name, appDescription: description }
                                : m
                        )
                    );
                }
                break;
            }
            case "logo_generated": {
                const uuid = parsed.uuid as string;
                if (uuid && streamingMessageIdRef.current) {
                    const logoUrl = `http://100.101.157.4:8080/api/logo/${uuid}`;
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, logoUrl }
                                : m
                        )
                    );
                }
                break;
            }
            case "app_generated": {
                const uuid = parsed.uuid as string;
                if (uuid && streamingMessageIdRef.current) {
                    const previewUrl = `http://100.101.157.4:8080/api/preview/${uuid}`;
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, previewUrl, isStreaming: false }
                                : m
                        )
                    );
                }
                break;
            }
            case "done": {
                if (streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, isStreaming: false }
                                : m
                        )
                    );
                }
                setIsStreaming(false);
                break;
            }
            case "error": {
                const errMsg = (parsed.error as string) || eventData;
                if (streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, error: errMsg, isStreaming: false }
                                : m
                        )
                    );
                }
                setError(errMsg);
                setIsStreaming(false);
                break;
            }
            default:
                // 没有事件类型时作为 message 处理
                if (eventData && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, content: m.content + eventData }
                                : m
                        )
                    );
                }
        }
    }, []);
```

- [ ] **Step 4: 实现 sendMessage**

```typescript
    const sendMessage = useCallback(async (content: string) => {
        if (!sessionId || !content.trim() || isStreaming) return;

        // 取消之前的请求
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }
        abortControllerRef.current = new AbortController();

        const userMessage: ChatMessage = {
            id: Date.now(),
            sessionId,
            role: "user",
            content: content.trim(),
            createdAt: new Date().toISOString(),
        };

        const botMessageId = Date.now() + 1;
        const botMessage: ChatMessage = {
            id: botMessageId,
            sessionId,
            role: "assistant",
            content: "",
            createdAt: new Date().toISOString(),
            isStreaming: true,
        };

        // 添加用户消息和 AI 占位消息
        setMessages((prev) => [...prev, userMessage, botMessage]);
        streamingMessageIdRef.current = botMessageId;
        setIsStreaming(true);
        setError(null);

        try {
            const response = await sendMessageStream(
                content.trim(),
                sessionId,
                abortControllerRef.current.signal
            );

            if (!response.ok) {
                throw new Error(`Request failed: ${response.status}`);
            }

            await parseSSE(response);
        } catch (err) {
            if (err instanceof Error && err.name === "AbortError") {
                // 取消
                setMessages((prev) =>
                    prev.filter((m) => m.id !== botMessageId)
                );
            } else {
                const errMsg = err instanceof Error ? err.message : "Unknown error";
                setMessages((prev) =>
                    prev.map((m) =>
                        m.id === botMessageId
                            ? { ...m, error: errMsg, isStreaming: false }
                            : m
                    )
                );
                setError(errMsg);
            }
            setIsStreaming(false);
        }

        streamingMessageIdRef.current = null;
    }, [sessionId, isStreaming, parseSSE]);
```

- [ ] **Step 5: 实现 cancelStream 和返回**

```typescript
    const cancelStream = useCallback(() => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setIsStreaming(false);
            if (streamingMessageIdRef.current) {
                setMessages((prev) =>
                    prev.filter((m) => m.id !== streamingMessageIdRef.current)
                );
                streamingMessageIdRef.current = null;
            }
        }
    }, []);

    return {
        messages,
        isLoading,
        isStreaming,
        error,
        sendMessage,
        loadHistory,
        cancelStream,
    };
}
```

- [ ] **Step 6: 提交**

```bash
git add apps/web/src/hooks/useChat.ts
git commit -m "feat(web): add useChat hook with SSE support"
```

---

## Chunk 3: 组件层

### Task 3: 重写 yuanchuang.tsx

**Files:**
- Modify: `apps/web/src/pages/yuanchuang.tsx`

- [ ] **Step 1: 简化导入和状态**

```typescript
import { useState, useRef, useEffect } from "react";
import { useAppStore } from "@/stores/app-store";
import { useChat, type SSEIntent } from "@/hooks/useChat";
import { GenMessageCard, AppInfoCard, AppPreviewCard } from "@/components/ai-elements";
import { SendIcon, UserIcon, Loader2Icon } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import "streamdown/styles.css";
import type { ChatMessage } from "@/hooks/useChat";
```

- [ ] **Step 2: 组件实现**

```typescript
export default function YuanChuangPage() {
  const selectedSessionId = useAppStore((state) => state.selectedSessionId);
  const [inputText, setInputText] = useState("");

  const { messages, isLoading, isStreaming, error, sendMessage, loadHistory } = useChat(selectedSessionId);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // 发送消息
  const handleSendMessage = async () => {
    if (!inputText.trim() || isStreaming) return;
    const text = inputText.trim();
    setInputText("");
    await sendMessage(text);
  };

  // 键盘事件
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && inputText.trim() && !isStreaming) {
      handleSendMessage();
    }
  };

  // 未选择会话
  if (!selectedSessionId) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-800">元创</h1>
          <p className="text-gray-500 mt-2">请选择一个会话开始聊天</p>
        </div>
      </div>
    );
  }

  // 加载中
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-gray-400">加载中...</div>
      </div>
    );
  }

  // 渲染消息
  const renderMessage = (msg: ChatMessage) => {
    if (msg.role === "user") {
      return (
        <div key={msg.id} className="flex gap-3 flex-row-reverse">
          <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0">
            <UserIcon className="h-4 w-4 text-white" />
          </div>
          <div className="max-w-[80%] rounded-lg px-4 py-2 bg-blue-500 text-white text-sm whitespace-pre-wrap">
            {msg.content}
          </div>
        </div>
      );
    }

    // assistant 消息
    // 如果有 app 信息（gen/edit 模式）
    if (msg.appName || msg.previewUrl) {
      return (
        <GenMessageCard
          key={msg.id}
          chatBeforeGen={msg.content}
          plan={msg.plan}
          appName={msg.appName}
          appDescription={msg.appDescription}
          logoUrl={msg.logoUrl}
          previewUrl={msg.previewUrl}
          isStreaming={msg.isStreaming}
        />
      );
    }

    // 普通对话
    return (
      <Streamdown
        key={msg.id}
        plugins={{ code, mermaid, math, cjk }}
        isAnimating={msg.isStreaming}
      >
        {msg.content}
      </Streamdown>
    );
  };

  return (
    <div className="flex flex-col h-full">
      {/* 消息列表 */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {messages.length === 0 ? (
          <div className="flex items-center justify-center h-full text-gray-400">
            开始你的对话
          </div>
        ) : (
          <div className="space-y-3">
            {messages.map((msg) => renderMessage(msg))}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* 输入框 */}
      <div className="px-4 py-3">
        <div className="flex gap-2">
          <Input
            className="p-4"
            placeholder="输入你的想法..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={isStreaming}
          />
          <Button
            size="icon"
            onClick={handleSendMessage}
            disabled={isStreaming || !inputText.trim()}
          >
            {isStreaming ? (
              <Loader2Icon className="h-8 w-8 animate-spin" />
            ) : (
              <SendIcon className="h-8 w-8" />
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/yuanchuang.tsx
git commit -m "refactor(web): simplify yuanchuang with useChat hook"
```

---

## Chunk 4: 验证

### Task 4: 测试验证

- [ ] **Step 1: 启动后端**

```bash
cd apps/api && ./mvnw spring-boot:run
```

- [ ] **Step 2: 启动前端**

```bash
cd apps/web && npm run dev
```

- [ ] **Step 3: 测试场景**

1. 登录用户
2. 选择或创建会话
3. 发送一条普通对话消息
4. 验证消息显示正常
5. 发送"生成一个计算器"类似的消息
6. 验证 SSE 事件按顺序显示：
   - intent 事件 → 显示"生成应用"标签
   - message 事件 → 显示聊天内容
   - plan 事件 → 显示 PlanCard
   - app_info 事件 → 显示 AppInfoCard
   - logo_generated 事件 → Logo 出现
   - app_generated 事件 → 预览出现
   - done 事件 → 流式结束

- [ ] **Step 4: 提交验证**

```bash
git add -A
git commit -m "test(web): verify SSE streaming works correctly"
```