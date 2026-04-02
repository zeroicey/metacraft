import http, { type ApiResponse } from "@/lib/http";
import type {
    ChatSessionCreateRequest,
    ChatSessionUpdateRequest,
    ChatSession,
    ChatMessage,
} from "@/types/session";

/**
 * Create a new chat session
 * @param data Session creation data
 * @returns Created session
 */
export const createSession = async (
    data: ChatSessionCreateRequest
): Promise<ChatSession> => {
    const response = await http
        .post("ai/sessions", { json: data })
        .json<ApiResponse<ChatSession>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to create session");
    }
    return response.data;
};

/**
 * Get all chat sessions for current user
 * @returns List of chat sessions
 */
export const getUserSessions = async (): Promise<ChatSession[]> => {
    const response = await http
        .get("ai/sessions")
        .json<ApiResponse<ChatSession[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch sessions");
    }
    return response.data;
};

/**
 * Get a chat session by ID
 * @param sessionId Session ID
 * @returns Chat session
 */
export const getSession = async (sessionId: string): Promise<ChatSession> => {
    const response = await http
        .get(`ai/sessions/${sessionId}`)
        .json<ApiResponse<ChatSession>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch session");
    }
    return response.data;
};

/**
 * Update a chat session
 * @param sessionId Session ID
 * @param data Session update data
 * @returns Updated session
 */
export const updateSession = async (
    sessionId: string,
    data: ChatSessionUpdateRequest
): Promise<ChatSession> => {
    const response = await http
        .patch(`ai/sessions/${sessionId}`, { json: data })
        .json<ApiResponse<ChatSession>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to update session");
    }
    return response.data;
};

/**
 * Delete a chat session
 * @param sessionId Session ID
 */
export const deleteSession = async (sessionId: string): Promise<void> => {
    const response = await http
        .delete(`ai/sessions/${sessionId}`)
        .json<ApiResponse<void>>();
    if (!response.data && response.message) {
        throw new Error(response.message || "Failed to delete session");
    }
};

/**
 * Get all messages in a chat session
 * @param sessionId Session ID
 * @returns List of chat messages
 */
export const getSessionMessages = async (
    sessionId: string
): Promise<ChatMessage[]> => {
    const response = await http
        .get(`ai/sessions/${sessionId}/messages`)
        .json<ApiResponse<ChatMessage[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch messages");
    }
    return response.data;
};