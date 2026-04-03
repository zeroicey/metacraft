import { useState, useRef, useEffect } from "react";
import { useYuanMengChat, type YuanMengMessage } from "@/hooks/useYuanMengChat";
import { useCurrentUser } from "@/hooks/useUser";
import { Loader2Icon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import "streamdown/styles.css";

// 元梦欢迎语（通用智能体）- 俏皮版
const yuanMengWelcomeMessages = [
  "你好呀！我是元梦 AI",
  "写代码、查资料、聊聊天，我都在行~",
  "有什么想问的尽管说，随时待命！",
  "对了，我还会写诗和讲故事哦",
  "想试试吗？快来和我聊天吧",
];

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
};

// 消息气泡
const MessageBubble = ({ message, avatarUrl, userName }: { message: YuanMengMessage; avatarUrl: string; userName: string }) => {
  const isUser = message.type === "user_message";

  if (isUser) {
    return (
      <div key={message.id} className="flex gap-3 flex-row-reverse">
        <img
          src={avatarUrl}
          alt={userName || "用户"}
          className="w-9 h-9 rounded-full object-cover flex-shrink-0 shadow-md"
        />
        <div className="max-w-[80%]">
          <div className="rounded-[14px_14px_4px_14px] px-4 py-3 bg-gradient-to-br from-[#EC4899] to-[#F472B6] text-white text-sm whitespace-pre-wrap shadow-md">
            {message.content}
          </div>
          <div className="text-xs text-gray-400 mt-1 text-right">
            {formatTime(message.timestamp)}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div key={message.id} className="max-w-[85%] border border-[#FCE7F3] rounded-[14px_14px_14px_4px] p-4 bg-white shadow-sm mr-auto">
      <Streamdown
        plugins={{ code, mermaid, math, cjk }}
        animated={false}
      >
        {message.content}
      </Streamdown>
      <div className="flex gap-3 text-xs text-gray-400 mt-2 pt-2 border-t border-[#FCE7F3]">
        <span>{formatTime(message.timestamp)}</span>
        {message.tokenCount && <span>消耗 {message.tokenCount} tokens</span>}
      </div>
    </div>
  );
};

export default function YuanMengPage() {
  const [inputText, setInputText] = useState("");
  const { messages, connectionStatus, sendMessage } = useYuanMengChat();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { data: user } = useCurrentUser();

  // 打字机效果（带删除效果和循环）
  const [displayMessage, setDisplayMessage] = useState("");
  const [messageIndex, setMessageIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    if (messages.length > 0) {
      setDisplayMessage("");
      setMessageIndex(0);
      setCharIndex(0);
      setIsDeleting(false);
      return;
    }

    const currentMessage = yuanMengWelcomeMessages[messageIndex];

    if (isDeleting) {
      if (charIndex > 0) {
        const timeout = setTimeout(() => {
          setDisplayMessage(prev => prev.slice(0, -1));
          setCharIndex(prev => prev - 1);
        }, 30);
        return () => clearTimeout(timeout);
      } else {
        setIsDeleting(false);
        setMessageIndex(prev => (prev + 1) % yuanMengWelcomeMessages.length);
      }
    } else {
      if (charIndex < currentMessage.length) {
        const timeout = setTimeout(() => {
          setDisplayMessage(prev => prev + currentMessage[charIndex]);
          setCharIndex(prev => prev + 1);
        }, 80);
        return () => clearTimeout(timeout);
      } else {
        const timeout = setTimeout(() => {
          setIsDeleting(true);
        }, 2000);
        return () => clearTimeout(timeout);
      }
    }
  }, [messages.length, messageIndex, charIndex, isDeleting]);

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

  const statusConfig = {
    connecting: { color: "bg-yellow-500", text: "连接中" },
    connected: { color: "bg-green-500", text: "已连接" },
    disconnected: { color: "bg-gray-400", text: "未连接" },
    error: { color: "bg-red-500", text: "连接错误" },
  }
  const status = statusConfig[connectionStatus]

  return (
    <div className="flex flex-col h-full relative">
      {/* 浮动连接状态 */}
      <div className="absolute top-4 right-4 z-10 flex items-center gap-1.5 text-xs text-gray-500 bg-white/80 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-md">
        <span className={`w-2 h-2 rounded-full ${status.color}`} />
        <span>{status.text}</span>
      </div>

      {/* 空状态：输入框居中 */}
      {messages.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center px-4 pb-4">
          {/* 桌面端欢迎语 */}
          <div className="hidden md:block text-center mb-8">
            <div className="text-2xl font-bold text-gray-800">
              {displayMessage}
              <span className="animate-pulse">|</span>
            </div>
          </div>
          {/* 手机端提示 */}
          <div className="md:hidden text-gray-400 mb-4">
            {isConnected ? "开始你的对话" : "等待连接..."}
          </div>
          {/* 输入框 */}
          <div className="w-full max-w-3xl">
            <div className="bg-white rounded-2xl border border-[#E5E7EB] focus-within:border-[#EC4899] focus-within:ring-2 focus-within:ring-[#EC4899]/20 transition-all">
              <textarea
                className="w-full p-3 border-0 rounded-t-2xl focus:outline-none resize-none max-h-32 field-sizing-content"
                placeholder="输入消息..."
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={!isConnected}
                rows={1}
              />
              <div className="flex items-center justify-between px-1 py-2">
                <div className="flex items-center gap-1">
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/>
                      <circle cx="12" cy="13" r="3"/>
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                      <circle cx="8.5" cy="8.5" r="1.5"/>
                      <polyline points="21 15 16 10 5 21"/>
                    </svg>
                  </Button>
                </div>
                <div className="flex items-center gap-1">
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                      <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                      <line x1="12" y1="19" x2="12" y2="23"/>
                      <line x1="8" y1="23" x2="16" y2="23"/>
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600" onClick={handleSendMessage} disabled={!isConnected || !inputText.trim()}>
                    {connectionStatus === "connecting" ? (
                      <Loader2Icon className="h-4 w-4 animate-spin" />
                    ) : (
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="22" y1="2" x2="11" y2="13"/>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                      </svg>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="flex-1 overflow-y-auto px-4 py-4 no-scrollbar">
          <div className="max-w-3xl mx-auto">
            <div className="space-y-4">
              {messages.map((msg) => (
                <MessageBubble key={msg.id} message={msg} avatarUrl={avatarUrl} userName={user?.name || "用户"} />
              ))}
              <div ref={messagesEndRef} />
            </div>
          </div>
        </div>
      )}

      {/* 有消息时的输入框 */}
      {messages.length > 0 && (
        <div className="p-4">
          <div className="max-w-3xl mx-auto">
            <div className="bg-white rounded-2xl border border-[#E5E7EB] focus-within:border-[#EC4899] focus-within:ring-2 focus-within:ring-[#EC4899]/20 transition-all">
              <textarea
                className="w-full p-3 border-0 rounded-t-2xl focus:outline-none resize-none max-h-32 field-sizing-content"
                placeholder="输入消息..."
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={!isConnected}
                rows={1}
              />
              <div className="flex items-center justify-between px-1 py-2">
                <div className="flex items-center gap-1">
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/>
                      <circle cx="12" cy="13" r="3"/>
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                      <circle cx="8.5" cy="8.5" r="1.5"/>
                      <polyline points="21 15 16 10 5 21"/>
                    </svg>
                  </Button>
                </div>
                <div className="flex items-center gap-1">
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                      <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                      <line x1="12" y1="19" x2="12" y2="23"/>
                      <line x1="8" y1="23" x2="16" y2="23"/>
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600" onClick={handleSendMessage} disabled={!isConnected || !inputText.trim()}>
                    {connectionStatus === "connecting" ? (
                      <Loader2Icon className="h-4 w-4 animate-spin" />
                    ) : (
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="22" y1="2" x2="11" y2="13"/>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                      </svg>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}