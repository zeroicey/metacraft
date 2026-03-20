import {
    useMutation,
    useQueryClient,
    useQuery,
} from "@tanstack/react-query";
import {
    createSession,
    deleteSession,
    getSession,
    getSessionMessages,
    getUserSessions,
    updateSession,
} from "@/api/session";
import type {
    ChatSession,
    ChatSessionCreateRequest,
    ChatSessionUpdateRequest,
} from "@/types/session";
import { toast } from "sonner";

/**
 * 创建聊天会话 mutation
 */
export function useCreateSession() {
    const queryClient = useQueryClient();

    return useMutation<ChatSession, Error, ChatSessionCreateRequest>({
        mutationFn: createSession,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["chat-sessions"] });
        },
        onError: (error) => {
            console.error("Failed to create session:", error);
            toast.error(error.message || "Failed to create session");
        },
    });
}

/**
 * 删除聊天会话 mutation
 */
export function useDeleteSession() {
    const queryClient = useQueryClient();

    return useMutation<void, Error, string>({
        mutationFn: deleteSession,
        onSuccess: () => {
            toast.success("Session deleted successfully!");
            // 刷新会话列表
            queryClient.invalidateQueries({ queryKey: ["chat-sessions"] });
        },
        onError: (error) => {
            console.error("Failed to delete session:", error);
            toast.error(error.message || "Failed to delete session");
        },
    });
}

/**
 * 更新聊天会话 mutation
 */
export function useUpdateSession() {
    const queryClient = useQueryClient();

    return useMutation<
        ChatSession,
        Error,
        { sessionId: string; data: ChatSessionUpdateRequest }
    >({
        mutationFn: ({ sessionId, data }) => updateSession(sessionId, data),
        onSuccess: (session) => {
            toast.success("Session updated successfully!");
            // 刷新会话列表
            queryClient.invalidateQueries({ queryKey: ["chat-sessions"] });
            // 刷新单个会话缓存
            queryClient.invalidateQueries({
                queryKey: ["chat-session", session.sessionId],
            });
        },
        onError: (error) => {
            console.error("Failed to update session:", error);
            toast.error(error.message || "Failed to update session");
        },
    });
}

/**
 * 获取用户所有会话列表
 * @param enabled 是否启用查询，默认 true
 */
export function useUserSessions(enabled: boolean = true) {
    return useQuery({
        queryKey: ["chat-sessions"],
        queryFn: getUserSessions,
        enabled,
    });
}

/**
 * 获取单个会话
 * @param sessionId 会话 ID
 * @param enabled 是否启用查询，默认 true
 */
export function useSession(sessionId: string, enabled: boolean = true) {
    return useQuery({
        queryKey: ["chat-session", sessionId],
        queryFn: () => getSession(sessionId),
        enabled: enabled && !!sessionId,
    });
}

/**
 * 获取会话消息列表
 * @param sessionId 会话 ID
 * @param enabled 是否启用查询，默认 true
 */
export function useSessionMessages(sessionId: string, enabled: boolean = true) {
    return useQuery({
        queryKey: ["chat-session-messages", sessionId],
        queryFn: () => getSessionMessages(sessionId),
        enabled: enabled && !!sessionId,
    });
}
