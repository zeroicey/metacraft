import http, { type ApiResponse } from "@/lib/http";

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
 * SSE 流式发送消息
 * @param message 消息内容
 * @param sessionId 会话 ID
 * @param signal AbortSignal
 */
export const sendMessageStream = async (
    message: string,
    sessionId: string,
    signal?: AbortSignal
): Promise<Response> => {
    return await http.post("ai/agent/unified", {
        json: { message, sessionId },
        headers: { Accept: "text/event-stream" },
        signal,
        stream: true,
    });
};