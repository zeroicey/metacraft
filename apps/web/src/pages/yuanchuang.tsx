import { Streamdown } from "streamdown"
import { useAppStore } from "@/stores/app-store"
import { useSessionMessages } from "@/hooks/useChatSession"
import { useChatStream } from "@/hooks/useChatStream"
import { SendIcon, UserIcon, Loader2Icon } from "lucide-react"
import { useState, useRef, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { toast } from "sonner"
import type { ChatMessage } from "@/types/session"
import { code } from '@streamdown/code';
import { mermaid } from '@streamdown/mermaid';
import { math } from '@streamdown/math';
import { cjk } from '@streamdown/cjk';
import "streamdown/styles.css";


export default function YuanChuangPage() {
  const selectedSessionId = useAppStore((state) => state.selectedSessionId)
  const [inputText, setInputText] = useState("")
  const [streamingContent, setStreamingContent] = useState("")
  const [localMessages, setLocalMessages] = useState<ChatMessage[]>([])
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const { data: messages = [], isLoading, refetch } = useSessionMessages(selectedSessionId)
  const { sendMessage, isStreaming, error } = useChatStream()

  // 合并本地消息和服务器消息（本地消息显示在最后）
  const allMessages = [...messages, ...localMessages]

  // 切换会话时清除本地消息
  useEffect(() => {
    setLocalMessages([])
    setStreamingContent("")
  }, [selectedSessionId])

  // 自动滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [allMessages, streamingContent])

  // 处理发送消息
  const handleSendMessage = async () => {
    if (!inputText.trim() || !selectedSessionId || isStreaming) return

    const messageText = inputText.trim()
    setInputText("")
    setStreamingContent("")

    // 立即添加用户消息到本地列表（乐观更新）
    const tempUserMessage: ChatMessage = {
      id: Date.now(),
      sessionId: selectedSessionId,
      role: "user",
      content: messageText,
      createdAt: new Date().toISOString(),
    }

    // 先更新 UI，让用户消息立即显示
    setLocalMessages((prev) => [...prev, tempUserMessage])

    // 强制刷新 UI
    await new Promise((resolve) => setTimeout(resolve, 0))

    // 然后发送消息
    sendMessage({
      message: messageText,
      sessionId: selectedSessionId,
      onMessage: (content) => {
        setStreamingContent((prev) => prev + content)
      },
      onIntent: (intent) => {
        console.log("Intent:", intent)
      },
      onDone: async () => {
        setStreamingContent("")
        // 清除本地消息，刷新服务器消息
        setLocalMessages([])
        await refetch()
      },
      onError: (errorMsg) => {
        toast.error(errorMsg)
        // 发送失败时移除本地消息
        setLocalMessages((prev) => prev.filter((m) => m.id !== tempUserMessage.id))
      },
    })
  }

  // 处理键盘事件
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && inputText.trim() && !isStreaming) {
      handleSendMessage()
    }
  }

  if (!selectedSessionId) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-800">元创</h1>
          <p className="text-gray-500 mt-2">请选择一个会话开始聊天</p>
        </div>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-gray-400">加载中...</div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      {/* 消息列表 */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {allMessages.length === 0 && !streamingContent ? (
          <div className="flex items-center justify-center h-full text-gray-400">
            开始你的对话
          </div>
        ) : (
          <div className="space-y-3">
            {allMessages.map((message) => (
              <div key={message.id}>
                {message.role === "user" ? (
                  <div className="flex gap-3 flex-row-reverse">
                    <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0">
                      <UserIcon className="h-4 w-4 text-white" />
                    </div>
                    <div className="max-w-[80%] rounded-lg px-4 py-2 bg-blue-500 text-white text-sm whitespace-pre-wrap">
                      {message.content}
                    </div>
                  </div>
                ) : (
                  <Streamdown>{message.content}</Streamdown>
                )}
              </div>
            ))}

            {/* 流式输出的内容 */}
            {streamingContent && (
              <Streamdown
                animated={{
                  animation: "blurIn",  // "fadeIn" | "blurIn" | "slideUp" | custom string
                  duration: 200,         // milliseconds (default: 150)
                  easing: "ease-out",    // CSS timing function (default: "ease")
                  sep: "word",           // "word" | "char" (default: "word")
                }}
                //caret="block"
                plugins={{
                  code: code,
                  mermaid: mermaid,
                  math: math,
                  cjk: cjk,
                }}
                isAnimating={isStreaming}>{streamingContent}</Streamdown>
            )}

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
          <Button size="icon" onClick={handleSendMessage} disabled={isStreaming || !inputText.trim()}>
            {isStreaming ? (
              <Loader2Icon className="h-8 w-8 animate-spin" />
            ) : (
              <SendIcon className="h-8 w-8" />
            )}
          </Button>
        </div>
      </div>
    </div>
  )
}
