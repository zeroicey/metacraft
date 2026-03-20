import { useState, useCallback, useRef } from "react";
import { parseServerSentEvents } from "parse-sse";
import http from "@/lib/http";

/** SSE 事件类型 */
export type SSEIntent = "chat" | "gen" | "edit";

/** 应用信息 */
export interface AppInfo {
    name: string;
    description: string;
}

/** Logo 生成数据 */
export interface LogoData {
    uuid: string;
    ext: string;
}

/** 应用生成数据 */
export interface AppGeneratedData {
    uuid: string;
    version: number;
}

/** 流式状态 */
export interface StreamState {
    isStreaming: boolean;
    intent: SSEIntent | null;
    error: string | null;
}

/** 发送消息选项 */
export interface SendMessageOptions {
    message: string;
    sessionId: string;
    onMessage?: (content: string) => void;
    onIntent?: (intent: SSEIntent) => void;
    onPlan?: (plan: string) => void;
    onAppInfo?: (info: AppInfo) => void;
    onLogoGenerated?: (data: LogoData) => void;
    onAppGenerated?: (data: AppGeneratedData) => void;
    onDone?: () => void;
    onError?: (error: string) => void;
}

/**
 * SSE 流式聊天 Hook
 * 处理消息发送和 SSE 事件解析
 */
export function useChatStream() {
    const [state, setState] = useState<StreamState>({
        isStreaming: false,
        intent: null,
        error: null,
    });
    const abortControllerRef = useRef<AbortController | null>(null);

    /**
     * 发送消息并接收 SSE 流式响应
     */
    const sendMessage = useCallback(async (options: SendMessageOptions) => {
        const { message, sessionId, onMessage, onIntent, onPlan, onAppInfo, onLogoGenerated, onAppGenerated, onDone, onError } = options;

        // 取消之前的请求
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }

        abortControllerRef.current = new AbortController();

        setState({
            isStreaming: true,
            intent: null,
            error: null,
        });

        try {
            const response = await http.post("ai/agent/unified", {
                json: {
                    message,
                    sessionId,
                },
                headers: {
                    Accept: "text/event-stream",
                },
                signal: abortControllerRef.current.signal,
                stream: true,
            });

            // 使用 parseServerSentEvents 解析 SSE 流
            const eventStream = parseServerSentEvents(response);

            for await (const event of eventStream) {
                console.log("[SSE] Event:", event.type, event.data);
                const eventType = event.type || "message";

                if (eventType === "intent") {
                    const data = event.data;
                    if (typeof data === "string" && (data === "chat" || data === "gen" || data === "edit")) {
                        const intent = data as SSEIntent;
                        setState((prev) => ({ ...prev, intent }));
                        onIntent?.(intent);
                    }
                } else if (eventType === "message") {
                    const data = event.data;
                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed.content === "string") {
                            onMessage?.(parsed.content);
                        }
                    } catch {
                        // 如果不是 JSON，直接使用原始数据
                        if (data && typeof data === "string") {
                            onMessage?.(data);
                        }
                    }
                } else if (eventType === "plan") {
                    const data = event.data;
                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed.plan === "string") {
                            onPlan?.(parsed.plan);
                        }
                    } catch {
                        if (data && typeof data === "string") {
                            onPlan?.(data);
                        }
                    }
                } else if (eventType === "app_info") {
                    const data = event.data;
                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed.name === "string" && typeof parsed.description === "string") {
                            const info: AppInfo = {
                                name: parsed.name,
                                description: parsed.description,
                            };
                            onAppInfo?.(info);
                        }
                    } catch (e) {
                        console.error("Failed to parse app_info:", e);
                    }
                } else if (eventType === "logo_generated") {
                    const data = event.data;
                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed.uuid === "string" && typeof parsed.ext === "string") {
                            const logoInfo: LogoData = {
                                uuid: parsed.uuid,
                                ext: parsed.ext,
                            };
                            onLogoGenerated?.(logoInfo);
                        }
                    } catch (e) {
                        console.error("Failed to parse logo_generated:", e);
                    }
                } else if (eventType === "app_generated") {
                    const data = event.data;
                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed.uuid === "string" && typeof parsed.version === "number") {
                            const appData: AppGeneratedData = {
                                uuid: parsed.uuid,
                                version: parsed.version,
                            };
                            onAppGenerated?.(appData);
                        }
                    } catch (e) {
                        console.error("Failed to parse app_generated:", e);
                    }
                } else if (eventType === "error") {
                    const data = event.data;
                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed.error === "string") {
                            const errorMsg = parsed.error;
                            setState((prev) => ({ ...prev, error: errorMsg }));
                            onError?.(errorMsg);
                        }
                    } catch {
                        if (data && typeof data === "string") {
                            setState((prev) => ({ ...prev, error: data }));
                            onError?.(data);
                        }
                    }
                } else if (eventType === "done") {
                    onDone?.();
                }
                // 其他事件 (plan, app_generated 等) 暂不处理
            }

            setState((prev) => ({ ...prev, isStreaming: false }));
        } catch (error: unknown) {
            const err = error instanceof Error ? error : new Error(String(error));
            if (err.name === "AbortError") {
                // 用户取消请求，正常情况
                setState((prev) => ({ ...prev, isStreaming: false }));
                return;
            }
            console.error("SSE stream error:", err);
            setState((prev) => ({ ...prev, isStreaming: false, error: err.message }));
            onError?.(err.message);
        }
    }, []);

    /**
     * 取消当前流式请求
     */
    const cancel = useCallback(() => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            setState((prev) => ({ ...prev, isStreaming: false }));
        }
    }, []);

    return {
        ...state,
        sendMessage,
        cancel,
    };
}
