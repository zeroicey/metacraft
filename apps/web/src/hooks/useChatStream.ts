import { useState, useCallback, useRef } from "react";
import { parseServerSentEvents } from "parse-sse";
import { useAuthStore } from "@/stores/auth-store";

/** SSE 事件类型 */
export type SSEIntent = "chat" | "gen" | "edit";

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
    const { message, sessionId, onMessage, onIntent, onDone, onError } = options;

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
      const token = localStorage.getItem("token");
      const apiUrl = useAuthStore.getState().apiUrl || "http://192.168.5.4:8080/api";
      const url = `${apiUrl}/ai/agent/unified`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
          Accept: "text/event-stream",
        },
        body: JSON.stringify({
          message,
          sessionId,
        }),
        signal: abortControllerRef.current.signal,
      });

      if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
      }

      // 使用 parseServerSentEvents 解析 SSE 流
      const eventStream = parseServerSentEvents(response);

      for await (const event of eventStream) {
        console.log("[SSE] Event:", event.type, event.data);
        const eventType = event.type || "message";

        if (eventType === "intent") {
          const data = event.data as SSEIntent;
          setState((prev) => ({ ...prev, intent: data }));
          onIntent?.(data);
        } else if (eventType === "message") {
          const data = event.data;
          try {
            const parsed = JSON.parse(data);
            const content = parsed.content as string;
            if (content) {
              onMessage?.(content);
            }
          } catch {
            // 如果不是 JSON，直接使用原始数据
            if (data) {
              onMessage?.(data);
            }
          }
        } else if (eventType === "error") {
          const data = event.data;
          try {
            const parsed = JSON.parse(data);
            const errorMsg = parsed.error as string;
            setState((prev) => ({ ...prev, error: errorMsg }));
            onError?.(errorMsg);
          } catch {
            setState((prev) => ({ ...prev, error: data }));
            onError?.(data);
          }
        } else if (eventType === "done") {
          onDone?.();
        }
        // 其他事件 (plan, app_generated 等) 暂不处理
      }

      setState((prev) => ({ ...prev, isStreaming: false }));
    } catch (error: unknown) {
      const err = error as Error;
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