import { useState, useRef, useEffect } from "react";
import { useAppStore } from "@/stores/app-store";
import { useChat, type ChatMessage } from "@/hooks/useChat";
import { useCurrentUser } from "@/hooks/useUser";
import { useUserSessions } from "@/hooks/useChatSession";
import { GenMessageCard } from "@/components/ai-elements";
import { SendIcon, Loader2Icon } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import "streamdown/styles.css";

export default function YuanChuangPage() {
  const selectedSessionId = useAppStore((state) => state.selectedSessionId);
  const setSelectedSessionId = useAppStore((state) => state.setSelectedSessionId);
  const [inputText, setInputText] = useState("");

  const { data: user } = useCurrentUser();
  const { data: sessions = [] } = useUserSessions();
  const { messages, isLoading, isStreaming, sendMessage } = useChat(selectedSessionId);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 自动选择最新的会话（如果当前没有选中会话）
  useEffect(() => {
    if (!selectedSessionId && sessions.length > 0) {
      // 按 updatedAt 排序，取最新的
      const sorted = [...sessions].sort(
        (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
      );
      setSelectedSessionId(sorted[0].sessionId);
    }
  }, [sessions, selectedSessionId, setSelectedSessionId]);

  // 获取用户头像 URL
  const getAvatarUrl = () => {
    if (!user?.avatarBase64 || user.avatarBase64 === "") {
      const seed = encodeURIComponent(user?.name || "user")
      return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`
    }
    if (user.avatarBase64.startsWith("data:")) {
      return user.avatarBase64
    }
    return `data:image/png;base64,${user.avatarBase64}`
  }

  const avatarUrl = getAvatarUrl()

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
          <img
            src={avatarUrl}
            alt={user?.name || "用户"}
            className="w-8 h-8 rounded-full object-cover flex-shrink-0"
          />
          <div className="max-w-[80%] rounded-lg px-4 py-2 bg-[#007AFF] text-white text-sm whitespace-pre-wrap">
            {msg.content}
          </div>
        </div>
      );
    }

    // assistant 消息 - 有 app 信息时显示 GenMessageCard
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
        animated
        isAnimating={msg.isStreaming}
      >
        {msg.content}
      </Streamdown>
    );
  };

  return (
    <div className="flex flex-col h-full">
      {/* 消息列表 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 no-scrollbar">
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
