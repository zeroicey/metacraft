import { useState, useCallback, useRef, useEffect } from "react";
import { getMessages, sendMessageStream } from "@/api/chat";

/** SSE 事件类型 */
export type SSEIntent = "chat" | "gen" | "edit";

/** 聊天消息 */
export interface ChatMessage {
    id: number;
    sessionId: string;
    role: "user" | "assistant";
    content: string;
    createdAt: string;
    // 流式过程动态更新的字段
    intent?: SSEIntent;
    plan?: string;
    appName?: string;
    appDescription?: string;
    logoUrl?: string;
    previewUrl?: string;
    isStreaming?: boolean;
    error?: string;
}

interface UseChatReturn {
    messages: ChatMessage[];
    isLoading: boolean;
    isStreaming: boolean;
    error: string | null;
    sendMessage: (content: string) => Promise<void>;
    loadHistory: (sessionId: string) => Promise<void>;
    cancelStream: () => void;
}

export function useChat(sessionId: string): UseChatReturn {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isStreaming, setIsStreaming] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const abortControllerRef = useRef<AbortController | null>(null);
    const streamingMessageIdRef = useRef<number | null>(null);

    // 加载历史消息
    const loadHistory = useCallback(async (sid: string) => {
        if (!sid) return;
        setIsLoading(true);
        setError(null);
        try {
            const data = await getMessages(sid);
            // 映射后端字段到前端字段
            const mapped = data.map((m: any) => ({
                id: m.id,
                sessionId: m.sessionId,
                role: m.role,
                content: m.content,
                createdAt: m.createdAt,
                // 映射后端的 relatedApp* 字段
                appName: m.relatedAppName,
                appDescription: m.relatedAppDescription,
                logoUrl: m.relatedAppLogo ? `http://100.101.157.4:8080/api/logo/${m.relatedAppLogo.replace(/\.[^/.]+$/, '')}` : undefined,
                previewUrl: m.relatedAppUuid ? `http://100.101.157.4:8080/api/preview/${m.relatedAppUuid}` : undefined,
                plan: undefined, // 历史消息没有 plan
                isStreaming: false,
            }));
            setMessages(mapped);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to load messages");
        } finally {
            setIsLoading(false);
        }
    }, []);

    // 会话切换时加载
    useEffect(() => {
        if (sessionId) {
            loadHistory(sessionId);
        } else {
            setMessages([]);
        }
    }, [sessionId, loadHistory]);

    // SSE 解析（按 \n\n 分割）
    // 处理单个事件
    const handleEvent = useCallback((eventName: string, eventData: string) => {
        console.log("[SSE] Event:", eventName, "Data:", eventData);

        let parsed: Record<string, unknown> = {};
        try {
            parsed = JSON.parse(eventData);
        } catch {
            // 非 JSON
        }

        switch (eventName) {
            case "intent": {
                const intent = (parsed.intent as string) || eventData;
                if (intent && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, intent: intent as SSEIntent }
                                : m
                        )
                    );
                }
                break;
            }
            case "message": {
                const content = (parsed.content as string) || eventData;
                if (content && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, content: m.content + content }
                                : m
                        )
                    );
                }
                break;
            }
            case "plan": {
                const plan = (parsed.plan as string) || eventData;
                console.log("[SSE] Plan received:", plan);
                if (plan && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, plan: (m.plan || "") + plan }
                                : m
                        )
                    );
                }
                break;
            }
            case "app_info": {
                const name = (parsed.name as string) || "";
                const description = (parsed.description as string) || "";
                console.log("[SSE] AppInfo received:", name, description);
                if (streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, appName: name, appDescription: description }
                                : m
                        )
                    );
                }
                break;
            }
            case "logo_generated": {
                const uuid = parsed.uuid as string;
                if (uuid && streamingMessageIdRef.current) {
                    const logoUrl = `http://100.101.157.4:8080/api/logo/${uuid}`;
                    console.log("[SSE] Logo generated:", logoUrl);
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, logoUrl }
                                : m
                        )
                    );
                }
                break;
            }
            case "app_generated": {
                const uuid = parsed.uuid as string;
                if (uuid && streamingMessageIdRef.current) {
                    const previewUrl = `http://100.101.157.4:8080/api/preview/${uuid}`;
                    console.log("[SSE] App generated:", previewUrl);
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, previewUrl, isStreaming: false }
                                : m
                        )
                    );
                }
                break;
            }
            case "done": {
                console.log("[SSE] Done");
                if (streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, isStreaming: false }
                                : m
                        )
                    );
                }
                setIsStreaming(false);
                break;
            }
            case "error": {
                const errMsg = (parsed.error as string) || eventData;
                console.log("[SSE] Error:", errMsg);
                if (streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, error: errMsg, isStreaming: false }
                                : m
                        )
                    );
                }
                setError(errMsg);
                setIsStreaming(false);
                break;
            }
            default:
                // 没有事件类型时作为 message 处理
                if (eventData && streamingMessageIdRef.current) {
                    setMessages((prev) =>
                        prev.map((m) =>
                            m.id === streamingMessageIdRef.current
                                ? { ...m, content: m.content + eventData }
                                : m
                        )
                    );
                }
        }
    }, []);

    const parseSSE = useCallback(async (response: Response) => {
        const reader = response.body?.getReader();
        if (!reader) throw new Error("No reader");

        const decoder = new TextDecoder("utf-8");
        let buffer = "";

        try {
            while (true) {
                const { done, value } = await reader.read();
                if (done) {
                    if (buffer.trim()) {
                        // 处理最后的数据
                    }
                    break;
                }

                buffer += decoder.decode(value, { stream: true });

                // 按 \n\n 分割（标准 SSE 格式）
                const events = buffer.split("\n\n");
                buffer = events.pop() || "";

                for (const eventBlock of events) {
                    if (!eventBlock.trim()) continue;

                    const lines = eventBlock.split("\n");
                    let eventName = "";
                    let eventData = "";

                    for (const line of lines) {
                        const trimmed = line.trimStart();
                        if (trimmed.startsWith("event:")) {
                            eventName = trimmed.substring(6).trim();
                        } else if (trimmed.startsWith("data:")) {
                            let content = trimmed.substring(5);
                            if (content.startsWith(" ")) content = content.substring(1);
                            eventData = content;
                        }
                    }

                    // 根据事件类型处理
                    handleEvent(eventName, eventData);
                }
            }
        } finally {
            reader.releaseLock();
        }
    }, [handleEvent]);

    // sendMessage 函数
    const sendMessage = useCallback(async (content: string) => {
        if (!sessionId || !content.trim() || isStreaming) return;

        // 取消之前的请求
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }
        abortControllerRef.current = new AbortController();

        const userMessage: ChatMessage = {
            id: Date.now(),
            sessionId,
            role: "user",
            content: content.trim(),
            createdAt: new Date().toISOString(),
        };

        const botMessageId = Date.now() + 1;
        const botMessage: ChatMessage = {
            id: botMessageId,
            sessionId,
            role: "assistant",
            content: "",
            createdAt: new Date().toISOString(),
            isStreaming: true,
        };

        // 添加用户消息和 AI 占位消息
        setMessages((prev) => [...prev, userMessage, botMessage]);
        streamingMessageIdRef.current = botMessageId;
        setIsStreaming(true);
        setError(null);

        try {
            const response = await sendMessageStream(
                content.trim(),
                sessionId,
                abortControllerRef.current.signal
            );

            if (!response.ok) {
                throw new Error(`Request failed: ${response.status}`);
            }

            await parseSSE(response);
        } catch (err) {
            if (err instanceof Error && err.name === "AbortError") {
                // 取消
                setMessages((prev) =>
                    prev.filter((m) => m.id !== botMessageId)
                );
            } else {
                const errMsg = err instanceof Error ? err.message : "Unknown error";
                setMessages((prev) =>
                    prev.map((m) =>
                        m.id === botMessageId
                            ? { ...m, error: errMsg, isStreaming: false }
                            : m
                    )
                );
                setError(errMsg);
            }
            setIsStreaming(false);
        }

        streamingMessageIdRef.current = null;
    }, [sessionId, isStreaming, parseSSE]);

    // cancelStream 函数
    const cancelStream = useCallback(() => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setIsStreaming(false);
            if (streamingMessageIdRef.current) {
                setMessages((prev) =>
                    prev.filter((m) => m.id !== streamingMessageIdRef.current)
                );
                streamingMessageIdRef.current = null;
            }
        }
    }, []);

    return {
        messages,
        isLoading,
        isStreaming,
        error,
        sendMessage,
        loadHistory,
        cancelStream,
    };
}