import { Streamdown } from "streamdown"
import { useAppStore } from "@/stores/app-store"
import { useSessionMessages } from "@/hooks/useChatSession"
import { useChatStream } from "@/hooks/useChatStream"
import { GenMessageCard, AppInfoCard, AppPreviewCard } from "@/components/ai-elements"
import { SendIcon, UserIcon, Loader2Icon } from "lucide-react"
import { useState, useRef, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { toast } from "sonner"
import type { ChatMessage } from "@/types/session"
import type { AppInfo, LogoData, AppGeneratedData, SSEIntent } from "@/hooks/useChatStream"
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

  // SSE 事件相关状态
  const [currentIntent, setCurrentIntent] = useState<SSEIntent | null>(null);
  const [planContent, setPlanContent] = useState("");
  const [appInfo, setAppInfo] = useState<{ name: string; description: string } | null>(null);
  const [logoData, setLogoData] = useState<{ uuid: string; ext: string } | null>(null);
  const [appGeneratedData, setAppGeneratedData] = useState<{ uuid: string; version: number } | null>(null);

  // 计算 Logo URL
  const logoUrl = logoData
    ? `http://100.101.157.4:8080/api/logo/${logoData.uuid}`
    : undefined;

  // 计算 Preview URL
  const previewUrl = appGeneratedData
    ? `/api/preview/${appGeneratedData.uuid}`
    : undefined;

  const { data: messages = [], isLoading, refetch } = useSessionMessages(selectedSessionId)
  const { sendMessage, isStreaming, error } = useChatStream()

  // 合并本地消息和服务器消息（本地消息显示在最后）
  const allMessages = [...messages, ...localMessages]

  // 切换会话时清除本地消息
  useEffect(() => {
    setLocalMessages([])
    setStreamingContent("")
    setCurrentIntent(null)
    setPlanContent("")
    setAppInfo(null)
    setLogoData(null)
    setAppGeneratedData(null)
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
        setCurrentIntent(intent)
      },
      onPlan: (plan) => {
        setPlanContent((prev) => prev + plan)
      },
      onAppInfo: (info) => {
        setAppInfo(info)
      },
      onLogoGenerated: (data) => {
        setLogoData(data)
      },
      onAppGenerated: (data) => {
        setAppGeneratedData(data)
      },
      onDone: () => {
        // 流式结束，清除临时状态
        // 注意：不调用 refetch() 避免页面滚动
        // 下次进入会话时会自动刷新消息
        setStreamingContent("")
        setCurrentIntent(null)
        setPlanContent("")
        setAppInfo(null)
        setLogoData(null)
        setAppGeneratedData(null)
        setLocalMessages([])
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
                  // 有应用信息时显示 GenMessageCard，否则显示普通消息
                  message.relatedAppUuid ? (
                    <GenMessageCard
                      chatBeforeGen={message.content}
                      plan=""
                      appName={message.relatedAppName}
                      appDescription={message.relatedAppDescription}
                      logoUrl={message.relatedAppLogo ? `http://100.101.157.4:8080/api/logo/${message.relatedAppLogo.replace(/\.[^/.]+$/, '')}` : undefined}
                      previewUrl={`http://100.101.157.4:8080/api/preview/${message.relatedAppUuid}`}
                      isStreaming={false}
                    />
                  ) : (
                    <Streamdown>{message.content}</Streamdown>
                  )
                )}
              </div>
            ))}

            {/* 流式输出的内容 */}
            {(streamingContent || planContent || previewUrl) && (
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
