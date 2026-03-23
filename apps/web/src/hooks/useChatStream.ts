import { useState, useCallback, useRef } from "react";
import http from "@/lib/http";

/** SSE 事件类型 */
export type SSEIntent = "chat" | "gen" | "edit";

/** 流式状态 */
export interface StreamState {
    isStreaming: boolean;
    intent: SSEIntent | null;
    error: string | null;
}

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
 * 使用自定义解析器，支持并行事件流
 */
export function useChatStream() {
    const [state, setState] = useState<StreamState>({
        isStreaming: false,
        intent: null,
        error: null,
    });
    const abortControllerRef = useRef<AbortController | null>(null);

    // 存储临时状态
    const appInfoRef = useRef<{ name: string; description: string } | null>(null);

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

        // 重置临时状态
        appInfoRef.current = null;

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
            }) as unknown as Response;

            // 使用自定义 SSE 解析器
            await parseSSEResponse(response, {
                onIntent: (intent) => {
                    if (intent === "chat" || intent === "gen" || intent === "edit") {
                        setState((prev) => ({ ...prev, intent }));
                        onIntent?.(intent);
                    }
                },
                onMessage: (content) => {
                    onMessage?.(content);
                },
                onPlan: (plan) => {
                    onPlan?.(plan);
                },
                onAppInfo: (name, description) => {
                    appInfoRef.current = { name, description };
                    onAppInfo?.({ name, description });
                },
                onLogoGenerated: (logoUrl: string) => {
                    const uuid = logoUrl.split("/").pop() || "";
                    onLogoGenerated?.({ uuid, ext: "png" });
                },
                onAppGenerated: (previewUrl: string) => {
                    const uuid = previewUrl.split("/").pop() || "";
                    onAppGenerated?.({ uuid, version: 1 });
                },
                onDone: () => {
                    onDone?.();
                },
                onError: (errorMsg) => {
                    setState((prev) => ({ ...prev, error: errorMsg }));
                    onError?.(errorMsg);
                },
            });

            setState((prev) => ({ ...prev, isStreaming: false }));
        } catch (error: unknown) {
            const err = error instanceof Error ? error : new Error(String(error));
            if (err.name === "AbortError") {
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

/**
 * SSE 解析状态
 */
interface SSEState {
    currentEvent: string;
    dataBuffer: string;
}

/**
 * 自定义 SSE 解析器 - 按行处理
 */
async function parseSSEResponse(
    response: Response,
    callbacks: {
        onIntent?: (intent: string) => void;
        onMessage?: (content: string) => void;
        onPlan?: (plan: string) => void;
        onAppInfo?: (name: string, description: string) => void;
        onLogoGenerated?: (logoUrl: string) => void;
        onAppGenerated?: (previewUrl: string) => void;
        onDone?: () => void;
        onError?: (error: string) => void;
    }
): Promise<void> {
    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
        throw new Error("Response body is null");
    }

    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let state: SSEState = { currentEvent: "", dataBuffer: "" };

    try {
        while (true) {
            const { done, value } = await reader.read();

            if (done) {
                // 处理剩余的 buffer
                if (buffer.trim()) {
                    processLine(buffer, state, callbacks);
                }
                break;
            }

            buffer += decoder.decode(value, { stream: true });

            // 按换行符分割
            const lines = buffer.split("\n");
            buffer = lines.pop() || "";

            for (const line of lines) {
                processLine(line, state, callbacks);
            }
        }
    } finally {
        reader.releaseLock();
    }
}

/**
 * 处理单行 SSE 数据
 */
function processLine(
    line: string,
    state: SSEState,
    callbacks: {
        onIntent?: (intent: string) => void;
        onMessage?: (content: string) => void;
        onPlan?: (plan: string) => void;
        onAppInfo?: (name: string, description: string) => void;
        onLogoGenerated?: (logoUrl: string) => void;
        onAppGenerated?: (previewUrl: string) => void;
        onDone?: () => void;
        onError?: (error: string) => void;
    }
): void {
    const trimmedLine = line.trim();
    if (!trimmedLine) return;

    if (trimmedLine.startsWith("event:")) {
        // 更新当前事件类型
        state.currentEvent = trimmedLine.substring(6).trim();
    } else if (trimmedLine.startsWith("data:")) {
        // 提取数据
        let data = trimmedLine.substring(5);
        if (data.startsWith(" ")) {
            data = data.substring(1);
        }

        // 根据当前事件类型处理数据
        handleData(state.currentEvent, data, callbacks);

        // 重置事件类型（根据 SSE 规范，data 行后事件类型应清空）
        // 但为了支持多行数据，我们保持状态到下一个 event 行
    }
}

/**
 * 处理数据
 */
function handleData(
    eventType: string,
    data: string,
    callbacks: {
        onIntent?: (intent: string) => void;
        onMessage?: (content: string) => void;
        onPlan?: (plan: string) => void;
        onAppInfo?: (name: string, description: string) => void;
        onLogoGenerated?: (logoUrl: string) => void;
        onAppGenerated?: (previewUrl: string) => void;
        onDone?: () => void;
        onError?: (error: string) => void;
    }
): void {
    if (!data) return;

    // 尝试解析 JSON
    let parsed: Record<string, unknown> = {};
    try {
        parsed = JSON.parse(data);
    } catch {
        // 不是 JSON，直接使用原始数据
    }

    // 根据事件类型调用回调
    switch (eventType) {
        case "intent": {
            const intent = parsed.intent as string || data;
            if (intent) callbacks.onIntent?.(intent);
            break;
        }
        case "message": {
            const content = parsed.content as string || data;
            if (content) callbacks.onMessage?.(content);
            break;
        }
        case "plan": {
            const plan = parsed.plan as string || data;
            if (plan) callbacks.onPlan?.(plan);
            break;
        }
        case "app_info": {
            const name = parsed.name as string || "";
            const description = parsed.description as string || "";
            if (name || description) callbacks.onAppInfo?.(name, description);
            break;
        }
        case "logo_generated": {
            const uuid = parsed.uuid as string;
            if (uuid) {
                const logoUrl = `http://localhost:8080/api/logo/${uuid}`;
                callbacks.onLogoGenerated?.(logoUrl);
            }
            break;
        }
        case "app_generated": {
            const uuid = parsed.uuid as string;
            if (uuid) {
                const previewUrl = `http://localhost:8080/api/preview/${uuid}`;
                callbacks.onAppGenerated?.(previewUrl);
            }
            break;
        }
        case "done": {
            callbacks.onDone?.();
            break;
        }
        case "error": {
            const error = parsed.error as string || data;
            if (error) callbacks.onError?.(error);
            break;
        }
        default:
            // 没有事件类型时，作为 message 处理
            if (data) {
                const content = (parsed as Record<string, unknown>).content as string;
                if (content) {
                    callbacks.onMessage?.(content);
                } else {
                    callbacks.onMessage?.(data);
                }
            }
            break;
    }
}
