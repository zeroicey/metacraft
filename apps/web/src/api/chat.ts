import http, { type ApiResponse } from "@/lib/http";
import { API_PREFIX_URL } from "@/lib/config";

/**
 * 获取会话消息
 * @param sessionId 会话 ID
 */
export const getMessages = async (sessionId: string) => {
    const response = await http
        .get(`ai/sessions/${sessionId}/messages`)
        .json<ApiResponse<any[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch messages");
    }
    return response.data;
};

/**
 * SSE 流式发送消息 - 使用原生 fetch
 * @param message 消息内容
 * @param sessionId 会话 ID
 * @param signal AbortSignal
 */
export const sendMessageStream = async (
    message: string,
    sessionId: string,
    signal?: AbortSignal
): Promise<Response> => {
    const token = localStorage.getItem("token");
    return await fetch(`${API_PREFIX_URL}/ai/agent/unified`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Accept": "text/event-stream",
            ...(token ? { "Authorization": `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ message, sessionId }),
        signal,
    });
};