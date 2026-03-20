# 元创页面聊天功能重新设计

> **Goal:** 重构 yuanchuang 页面的消息处理，移除 TanStack Query，参考 Huawei 客户端实现 SSE 流式显示

## Architecture

使用 `useState + 自定义 hook` 模式，不依赖任何第三方状态管理库。

- **API 层**: 直接使用 `ky` HTTP 客户端调用后端接口
- **Hook 层**: `useChat` hook 封装所有聊天逻辑（消息列表、加载状态、SSE 连接）
- **组件层**: `yuanchuang.tsx` 简化为纯 UI，调用 useChat hook

## Data Flow

```
用户发送消息
    ↓
创建用户消息 + AI占位消息（placeholder）
    ↓
SSE 连接开始，实时更新占位消息的各个字段
    ↓
SSE 事件: intent → message → plan → app_info → logo_generated → app_generated → done
    ↓
每个事件到达时直接更新本地消息状态
    ↓
切换会话 → 重新从 API 加载历史消息
```

## SSE Parsing (参考 Huawei SSEClient.ets)

```typescript
// 按 \n\n 分割每个完整事件消息
buffer.split('\n\n')

// 在每个块内解析 event: 和 data: 行
// 根据 event 类型调用对应回调
```

事件类型处理：
- `intent`: 设置意图（chat/gen/edit）
- `message`: 追加聊天内容
- `plan`: 设置生成计划
- `app_info`: 设置应用名称和描述
- `logo_generated`: 设置 Logo URL
- `app_generated`: 设置预览 URL
- `done`: 完成流式
- `error`: 错误处理

## File Structure

```
src/
├── api/
│   └── chat.ts              # 聊天 API（替换 session.ts 的部分功能）
├── hooks/
│   └── useChat.ts           # 核心 hook（替换 useChatSession + useChatStream）
├── pages/
│   └── yuanchuang.tsx       # 简化后的页面组件
```

## API Design

### chat.ts

```typescript
// 获取会话列表
export const getSessions = () => http.get('ai/sessions').json()

// 获取单个会话
export const getSession = (sessionId: string) => http.get(`ai/sessions/${sessionId}`).json()

// 获取会话消息
export const getMessages = (sessionId: string) => http.get(`ai/sessions/${sessionId}/messages`).json()

// 创建会话
export const createSession = (title: string) => http.post('ai/sessions', { json: { title } }).json()

// 删除会话
export const deleteSession = (sessionId: string) => http.delete(`ai/sessions/${sessionId}`).json()

// SSE 流式发送消息（返回 ReadableStream）
export const sendMessageStream = (message: string, sessionId: string, signal?: AbortSignal): Promise<Response>
```

### useChat.ts

```typescript
interface UseChatReturn {
  // 状态
  messages: ChatMessage[]
  isLoading: boolean
  isStreaming: boolean
  error: string | null
  currentIntent: SSEIntent | null

  // 方法
  sendMessage: (content: string) => Promise<void>
  loadHistory: (sessionId: string) => Promise<void>
  cancelStream: () => void
}
```

## Message State Structure

```typescript
interface ChatMessage {
  id: number
  sessionId: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string

  // 流式过程中动态更新的字段
  intent?: SSEIntent
  plan?: string
  appName?: string
  appDescription?: string
  logoUrl?: string
  previewUrl?: string
  isStreaming?: boolean
  error?: string
}
```

## Component Implementation (yuanchuang.tsx)

```typescript
const { messages, isLoading, isStreaming, sendMessage, loadHistory, cancelStream } = useChat(sessionId)

useEffect(() => {
  if (sessionId) loadHistory(sessionId)
}, [sessionId])

// 渲染消息列表
{messages.map(msg => (
  <MessageItem key={msg.id} message={msg} />
))}

// 流式内容直接在 messages 里的 assistant 消息中更新
```

## Key Differences from Current Implementation

| 方面 | 当前实现 | 新实现 |
|------|---------|--------|
| 状态管理 | TanStack Query | useState + useRef |
| SSE 处理 | useChatStream hook | 集成到 useChat |
| 消息存储 | 本地临时 + refetch | 本地直接更新 |
| 流式内容 | 单独状态 (streamingContent) | 合并到消息对象 |
| 组件复杂度 | yuanchuang.tsx 200+ 行 | 简化到 100 行内 |

## Implementation Order

1. 创建 `api/chat.ts` - 基础 API 函数
2. 创建 `hooks/useChat.ts` - 核心 hook（含 SSE 解析）
3. 重写 `pages/yuanchuang.tsx` - 使用新 hook
4. 测试验证各事件类型的显示

## References

- Huawei SSEClient: `apps/huawei/entry/src/main/ets/core/http/SSEClient.ets`
- Huawei ChatPanel: `apps/huawei/entry/src/main/ets/features/chat/components/ChatPanel.ets`