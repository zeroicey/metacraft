import { Streamdown } from "streamdown"
import { useAppStore } from "@/stores/app-store"
import { useSessionMessages } from "@/hooks/useChatSession"
import { SendIcon, UserIcon } from "lucide-react"
import { useState } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"

export default function YuanChuangPage() {
  const selectedSessionId = useAppStore((state) => state.selectedSessionId)
  const [inputText, setInputText] = useState("")
  const { data: messages = [], isLoading } = useSessionMessages(selectedSessionId)

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
        {messages.length === 0 ? (
          <div className="flex items-center justify-center h-full text-gray-400">
            开始你的对话
          </div>
        ) : (
          <div className="space-y-3">
            {messages.map((message) => (
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
            onKeyDown={(e) => {
              if (e.key === "Enter" && inputText.trim()) {
                // TODO: 发送消息
              }
            }}
          />
          <Button size="icon">
            <SendIcon className="h-8 w-8" />
          </Button>
        </div>
      </div>
    </div>
  )
}
