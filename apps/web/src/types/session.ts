/**
 * Chat session create request
 */
export interface ChatSessionCreateRequest {
    title: string;
    relatedAppId?: number;
}

/**
 * Chat session update request
 */
export interface ChatSessionUpdateRequest {
    title: string;
}

/**
 * Chat session data (aligned with backend ChatSessionVO)
 */
export interface ChatSession {
    sessionId: string;
    title: string;
    relatedAppId?: number;
    createdAt: string;
    updatedAt: string;
}

/**
 * Chat message role
 */
export type ChatMessageRole = "user" | "assistant";

/**
 * Chat message type (for app binding)
 */
export type ChatMessageType = "text" | "app";

/**
 * Chat message data (aligned with backend ChatMessageVO)
 */
export interface ChatMessage {
    id: number;
    sessionId: string;
    role: ChatMessageRole;
    content: string;
    createdAt: string;
    type?: ChatMessageType;
    relatedAppId?: number;
    relatedVersionId?: number;
    relatedAppUuid?: string;
    relatedAppName?: string;
    relatedAppDescription?: string;
    relatedAppLogo?: string;
}