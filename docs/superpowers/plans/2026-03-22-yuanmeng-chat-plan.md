# YuanMeng 聊天页面实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现前端的 yuanmeng 聊天页面，通过 WebSocket 与后端通信，无需登录。

**Architecture:** 使用 react-use-websocket 连接 WebSocket，在 yuanmeng.tsx 页面中实现聊天功能。消息存储在本地 state，用 Streamdown 渲染 Markdown。

**Tech Stack:** React, react-use-websocket, streamdown, lucide-react

---

## 文件结构

```
apps/web/src/
├── pages/yuanmeng.tsx              # 主页面（修改）
├── hooks/useYuanMengChat.ts        # 新建：WebSocket 聊天 hook
```

## 实现步骤

### Chunk 1: 安装依赖和创建 Hook

- [ ] **Step 1: 安装 react-use-websocket**

```bash
cd apps/web && bun add react-use-websocket
```

- [ ] **Step 2: 创建 useYuanMengChat hook**

创建文件 `apps/web/src/hooks/useYuanMengChat.ts`：

```typescript
import { useState, useCallback, useEffect, useRef } from "react";
import useWebSocket, { ReadyState } from "react-use-websocket";
import { API_BASE_URL } from "@/lib/config";

/** 消息类型 */
export interface YuanMengMessage {
    id: string;
    type: "user_message" | "assistant_message" | "progress" | "system";
    senderId: string;
    content: string;
    timestamp: number;
}

interface UseYuanMengChatReturn {
    messages: YuanMengMessage[];
    isConnected: boolean;
    connectionStatus: "connecting" | "connected" | "disconnected" | "error";
    sendMessage: (content: string) => void;
}

export function useYuanMengChat(): UseYuanMengChatReturn {
    const [messages, setMessages] = useState<YuanMengMessage[]>([]);
    const senderIdRef = useRef(`client-${Math.random().toString(36).substring(2, 9)}`);

    const { sendMessage: wsSendMessage, lastMessage, readyState } = useWebSocket(
        `${API_BASE_URL.replace("http", "ws")}/ws/yuanmeng/client`,
        {
            shouldReconnect: () => true,
            reconnectAttempts: 10,
            reconnectInterval: 3000,
        }
    );

    // 处理接收到的消息
    useEffect(() => {
        if (!lastMessage) return;

        try {
            const data = JSON.parse(lastMessage.data);
            const { type, senderId, content } = data;

            if (type === "user_message" || type === "assistant_message") {
                setMessages((prev) => [
                    ...prev,
                    {
                        id: `${Date.now()}-${Math.random()}`,
                        type: type === "user_message" ? "user_message" : "assistant_message",
                        senderId,
                        content: content || "",
                        timestamp: Date.now(),
                    },
                ]);
            }
        } catch (e) {
            console.error("Failed to parse WebSocket message:", e);
        }
    }, [lastMessage]);

    // 发送消息
    const sendMessage = useCallback(
        (content: string) => {
            if (readyState !== ReadyState.OPEN || !content.trim()) return;

            const payload = {
                type: "user_message",
                chatId: "shared",
                senderId: senderIdRef.current,
                content: content.trim(),
                metadata: { source: "client" },
            };

            wsSendMessage(JSON.stringify(payload));
        },
        [readyState, wsSendMessage]
    );

    const connectionStatus =
        readyState === ReadyState.OPEN
            ? "connected"
            : readyState === ReadyState.CONNECTING
            ? "connecting"
            : "disconnected";

    return {
        messages,
        isConnected: readyState === ReadyState.OPEN,
        connectionStatus,
        sendMessage,
    };
}
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/package.json apps/web/src/hooks/useYuanMengChat.ts
git commit -m "feat(web): add useYuanMengChat hook for WebSocket chat

- Add react-use-websocket dependency
- Create useYuanMengChat hook with WebSocket connection
- Handle message sending and receiving

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Chunk 2: 实现页面组件

- [ ] **Step 1: 修改 yuanmeng.tsx 页面**

修改 `apps/web/src/pages/yuanmeng.tsx`：

```typescript
import { useState, useRef, useEffect } from "react";
import { useYuanMengChat, type YuanMengMessage } from "@/hooks/useYuanMengChat";
import { SendIcon, UserIcon, BotIcon, Loader2Icon } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import "streamdown/styles.css";

// 连接状态指示器
const ConnectionIndicator = ({
    status,
}: {
    status: "connecting" | "connected" | "disconnected" | "error";
}) => {
    const statusConfig = {
        connecting: { color: "bg-yellow-500", text: "连接中..." },
        connected: { color: "bg-green-500", text: "已连接" },
        disconnected: { color: "bg-gray-400", text: "已断开" },
        error: { color: "bg-red-500", text: "连接错误" },
    };

    const config = statusConfig[status];

    return (
        <div className="flex items-center gap-2 text-sm text-gray-500">
            <span className={`w-2 h-2 rounded-full ${config.color}`} />
            <span>{config.text}</span>
        </div>
    );
};

// 消息气泡
const MessageBubble = ({ message }: { message: YuanMengMessage }) => {
    const isUser = message.type === "user_message";

    if (isUser) {
        return (
            <div key={message.id} className="flex gap-3 flex-row-reverse">
                <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0">
                    <UserIcon className="h-4 w-4 text-white" />
                </div>
                <div className="max-w-[80%] rounded-lg px-4 py-2 bg-blue-500 text-white text-sm whitespace-pre-wrap">
                    {message.content}
                </div>
            </div>
        );
    }

    return (
        <div key={message.id} className="flex gap-3">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center flex-shrink-0">
                <BotIcon className="h-4 w-4 text-white" />
            </div>
            <div className="max-w-[80%] rounded-lg px-4 py-2 bg-gray-100 text-gray-800 text-sm">
                <Streamdown plugins={{ code, mermaid, math, cjk }} animated={false}>
                    {message.content}
                </Streamdown>
            </div>
        </div>
    );
};

export default function YuanMengPage() {
    const [inputText, setInputText] = useState("");
    const { messages, connectionStatus, sendMessage } = useYuanMengChat();
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // 自动滚动到底部
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    // 发送消息
    const handleSendMessage = () => {
        if (!inputText.trim() || connectionStatus !== "connected") return;
        sendMessage(inputText.trim());
        setInputText("");
    };

    // 键盘事件
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter" && inputText.trim() && connectionStatus === "connected") {
            handleSendMessage();
        }
    };

    const isConnected = connectionStatus === "connected";

    return (
        <div className="flex flex-col h-full">
            {/* 消息列表 */}
            <div className="flex-1 overflow-y-auto px-4 py-4 no-scrollbar">
                {messages.length === 0 ? (
                    <div className="flex items-center justify-center h-full text-gray-400">
                        {isConnected ? "开始你的对话" : "等待连接..."}
                    </div>
                ) : (
                    <div className="space-y-3">
                        {messages.map((msg) => (
                            <MessageBubble key={msg.id} message={msg} />
                        ))}
                        <div ref={messagesEndRef} />
                    </div>
                )}
            </div>

            {/* 输入框 */}
            <div className="px-4 py-3 border-t border-gray-200">
                <div className="flex gap-2">
                    <Input
                        className="p-4"
                        placeholder="输入消息..."
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        onKeyDown={handleKeyDown}
                        disabled={!isConnected}
                    />
                    <Button
                        size="icon"
                        onClick={handleSendMessage}
                        disabled={!isConnected || !inputText.trim()}
                    >
                        {connectionStatus === "connecting" ? (
                            <Loader2Icon className="h-5 w-5 animate-spin" />
                        ) : (
                            <SendIcon className="h-5 w-5" />
                        )}
                    </Button>
                </div>
                <div className="mt-2">
                    <ConnectionIndicator status={connectionStatus} />
                </div>
            </div>
        </div>
    );
}
```

- [ ] **Step 2: 检查 BotIcon 是否存在**

如果 `lucide-react` 中没有 `BotIcon`，可以使用其他图标。检查：

```bash
grep -r "BotIcon" apps/web/src/ 2>/dev/null || echo "BotIcon not found"
```

如果不存在，用 `MessageSquareIcon` 替代。

- [ ] **Step 3: 验证编译**

```bash
cd apps/web && bun run build 2>&1 | head -20
```

- [ ] **Step 4: 提交**

```bash
git add apps/web/src/pages/yuanmeng.tsx
git commit -m "feat(web): implement yuanmeng chat page with WebSocket

- Add WebSocket connection using react-use-websocket
- Implement chat UI with Streamdown for Markdown rendering
- Add connection status indicator

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 验证

1. 启动后端：`cd apps/api && ./mvnw spring-boot:run`
2. 启动前端：`cd apps/web && bun run dev`
3. 访问 http://localhost:5173/yuanmeng
4. 发送消息，验证 WebSocket 连接和消息收发

Plan complete and saved to `docs/superpowers/plans/2026-03-22-yuanmeng-chat-plan.md`. Ready to execute?