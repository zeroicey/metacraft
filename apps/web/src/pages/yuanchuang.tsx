import { useState, useRef, useEffect } from "react";
import { useAppStore } from "@/stores/app-store";
import { useChat, type ChatMessage } from "@/hooks/useChat";
import { useCurrentUser } from "@/hooks/useUser";
import { useUserSessions } from "@/hooks/useChatSession";
import { GenMessageCard } from "@/components/ai-elements";
import { Loader2Icon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import "streamdown/styles.css";

// 元创欢迎语（AI生成应用）- 俏皮版
const yuanChuangWelcomeMessages = [
  "你好！我是源创 AI",
  "说出你的想法，我来帮你做网页应用",
  "比如：'帮我做个待办事项'、'来一个计算器'",
  "分分钟给你搞定，就是这么高效！",
  "准备好了吗？说出你想要的应用吧",
];

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
};

// 生成随机 token 数
const getRandomTokens = () => Math.floor(Math.random() * 500) + 100;

export default function YuanChuangPage() {
  const selectedSessionId = useAppStore((state) => state.selectedSessionId);
  const setSelectedSessionId = useAppStore((state) => state.setSelectedSessionId);
  const [inputText, setInputText] = useState("");

  const { data: user } = useCurrentUser();
  const { data: sessions = [] } = useUserSessions();
  const { messages, isLoading, isStreaming, sendMessage } = useChat(selectedSessionId);
  const messagesEndRef = useRef<HTMLDivElement>(null);

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

    const currentMessage = yuanChuangWelcomeMessages[messageIndex];

    if (isDeleting) {
      // 删除效果
      if (charIndex > 0) {
        const timeout = setTimeout(() => {
          setDisplayMessage(prev => prev.slice(0, -1));
          setCharIndex(prev => prev - 1);
        }, 30);
        return () => clearTimeout(timeout);
      } else {
        // 删除完毕，切换到下一条
        setIsDeleting(false);
        setMessageIndex(prev => (prev + 1) % yuanChuangWelcomeMessages.length);
      }
    } else {
      // 打字效果
      if (charIndex < currentMessage.length) {
        const timeout = setTimeout(() => {
          setDisplayMessage(prev => prev + currentMessage[charIndex]);
          setCharIndex(prev => prev + 1);
        }, 80);
        return () => clearTimeout(timeout);
      } else {
        // 打完等待后开始删除
        const timeout = setTimeout(() => {
          setIsDeleting(true);
        }, 2000);
        return () => clearTimeout(timeout);
      }
    }
  }, [messages.length, messageIndex, charIndex, isDeleting]);

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
            className="w-9 h-9 rounded-full object-cover flex-shrink-0 shadow-md"
          />
          <div className="max-w-[80%] px-4 py-3 bg-gradient-to-br from-[#007AFF] to-[#0056CC] text-white text-sm whitespace-pre-wrap rounded-[14px_14px_4px_14px] shadow-md">
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
      <div key={msg.id} className="max-w-[85%] border border-[#E8F0FE] rounded-[14px_14px_14px_4px] p-4 bg-white shadow-sm mr-auto">
        <Streamdown
          plugins={{ code, mermaid, math, cjk }}
          animated
          isAnimating={msg.isStreaming}
        >
          {msg.content}
        </Streamdown>
        <div className="flex gap-3 text-xs text-gray-400 mt-2 pt-2 border-t border-[#E8F0FE]">
          <span>{formatTime(Date.now())}</span>
          <span>消耗 {getRandomTokens()} tokens</span>
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full">
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
            开始你的对话
          </div>
          {/* 输入框 */}
          <div className="w-full max-w-3xl">
            <div className="bg-white rounded-2xl border border-[#E5E7EB] focus-within:border-[#007AFF] focus-within:ring-2 focus-within:ring-[#007AFF]/20 transition-all">
              <textarea
                className="w-full p-3 border-0 rounded-t-2xl focus:outline-none resize-none max-h-32 field-sizing-content"
                placeholder="输入你的想法..."
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isStreaming}
                rows={1}
              />
              <div className="flex items-center justify-between px-1 py-2">
                <div className="flex items-center gap-1">
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" />
                      <circle cx="12" cy="13" r="3" />
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                      <polyline points="14 2 14 8 20 8" />
                    </svg>
                  </Button>
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <polyline points="21 15 16 10 5 21" />
                    </svg>
                  </Button>
                </div>
                <div className="flex items-center gap-1">
                  <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                      <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                      <line x1="12" y1="19" x2="12" y2="23" />
                      <line x1="8" y1="23" x2="16" y2="23" />
                    </svg>
                  </Button>
                  <Button
                    size="icon"
                    variant="ghost"
                    className="h-8 w-8 text-gray-400 hover:text-gray-600"
                    onClick={handleSendMessage}
                    disabled={isStreaming || !inputText.trim()}
                  >
                    {isStreaming ? (
                      <Loader2Icon className="h-4 w-4 animate-spin" />
                    ) : (
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="22" y1="2" x2="11" y2="13" />
                        <polygon points="22 2 15 22 11 13 2 9 22 2" />
                      </svg>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <>
          {/* 消息列表 */}
          <div className="flex-1 overflow-y-auto px-4 py-4 no-scrollbar">
            <div className="max-w-3xl mx-auto">
              <div className="space-y-4">
                {messages.map((msg) => renderMessage(msg))}
                <div ref={messagesEndRef} />
              </div>
            </div>
          </div>

          {/* 输入框 */}
          <div className="p-4">
            <div className="max-w-3xl mx-auto">
              <div className="bg-white rounded-2xl border border-[#E5E7EB] focus-within:border-[#007AFF] focus-within:ring-2 focus-within:ring-[#007AFF]/20 transition-all">
                <textarea
                  className="w-full p-3 border-0 rounded-t-2xl focus:outline-none resize-none max-h-32 field-sizing-content"
                  placeholder="输入你的想法..."
                  value={inputText}
                  onChange={(e) => setInputText(e.target.value)}
                  onKeyDown={handleKeyDown}
                  disabled={isStreaming}
                  rows={1}
                />
                <div className="flex items-center justify-between px-1 py-2">
                  <div className="flex items-center gap-1">
                    <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" />
                        <circle cx="12" cy="13" r="3" />
                      </svg>
                    </Button>
                    <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                        <polyline points="14 2 14 8 20 8" />
                      </svg>
                    </Button>
                    <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                        <circle cx="8.5" cy="8.5" r="1.5" />
                        <polyline points="21 15 16 10 5 21" />
                      </svg>
                    </Button>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600">
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                        <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                        <line x1="12" y1="19" x2="12" y2="23" />
                        <line x1="8" y1="23" x2="16" y2="23" />
                      </svg>
                    </Button>
                    <Button size="icon" variant="ghost" className="h-8 w-8 text-gray-400 hover:text-gray-600" onClick={handleSendMessage} disabled={isStreaming || !inputText.trim()}>
                      {isStreaming ? (
                        <Loader2Icon className="h-4 w-4 animate-spin" />
                      ) : (
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <line x1="22" y1="2" x2="11" y2="13" />
                          <polygon points="22 2 15 22 11 13 2 9 22 2" />
                        </svg>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
