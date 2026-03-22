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
    tokenCount?: number;
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
                // AI 消息随机生成 token 数量
                const tokenCount = type === "assistant_message"
                    ? Math.floor(Math.random() * 500) + 50
                    : undefined;

                setMessages((prev) => [
                    ...prev,
                    {
                        id: `${Date.now()}-${Math.random()}`,
                        type: type === "user_message" ? "user_message" : "assistant_message",
                        senderId,
                        content: content || "",
                        timestamp: Date.now(),
                        tokenCount,
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
            : readyState === ReadyState.CLOSED || readyState === ReadyState.CLOSING
            ? "disconnected"
            : "error";

    return {
        messages,
        isConnected: readyState === ReadyState.OPEN,
        connectionStatus,
        sendMessage,
    };
}