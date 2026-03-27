import { useState, useRef, useEffect } from "react";
import { useYuanMengChat, type YuanMengMessage } from "@/hooks/useYuanMengChat";
import { useCurrentUser } from "@/hooks/useUser";
import { SendIcon, Loader2Icon } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import "streamdown/styles.css";

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

    return (
        <div className="flex flex-col h-full">
            {/* 消息列表 */}
            <div className="flex-1 overflow-y-auto px-4 py-4 no-scrollbar">
                {messages.length === 0 ? (
                    <div className="flex items-center justify-center h-full text-gray-400">
                        {isConnected ? "开始你的对话" : "等待连接..."}
                    </div>
                ) : (
                    <div className="space-y-4">
                        {messages.map((msg) => (
                            <MessageBubble key={msg.id} message={msg} avatarUrl={avatarUrl} userName={user?.name || "用户"} />
                        ))}
                        <div ref={messagesEndRef} />
                    </div>
                )}
            </div>

            {/* 输入框 */}
            <div className="px-4 py-3 bg-white rounded-2xl shadow-lg mx-4 mb-3">
                <div className="flex gap-3">
                    <Input
                        className="p-3 bg-[#F9FAFB] border border-[#E5E7EB] rounded-xl focus:border-[#EC4899] focus:ring-2 focus:ring-[#EC4899]/20 focus:bg-white transition-all"
                        placeholder="输入消息..."
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        onKeyDown={handleKeyDown}
                        disabled={!isConnected}
                    />
                    <Button
                        size="icon"
                        className="h-10 w-10 bg-gradient-to-br from-[#EC4899] to-[#BE185D] hover:from-[#BE185D] hover:to-[#9D174D] text-white rounded-xl shadow-md transition-all hover:scale-105 active:scale-95"
                        onClick={handleSendMessage}
                        disabled={!isConnected || !inputText.trim()}
                    >
                        {connectionStatus === "connecting" ? (
                            <Loader2Icon className="h-4 w-4 animate-spin" />
                        ) : (
                            <SendIcon className="h-4 w-4" />
                        )}
                    </Button>
                </div>
            </div>
        </div>
    );
}