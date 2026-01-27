/**
 * Chat Session Data Models
 */

// 会话信息 VO
export interface ChatSession {
  sessionId: string;
  title: string;
  createdAt: string; // ISO 8601 string
  updatedAt: string; // ISO 8601 string
}

// 消息信息 VO
export interface ChatMessage {
  id: number;
  sessionId: string;
  role: 'user' | 'assistant' | 'system'; // 限定 role 类型
  content: string;
  createdAt: string; // ISO 8601 string
}

// 创建会话请求 DTO
export interface CreateSessionRequest {
  title: string;
}

// 更新会话请求 DTO
export interface UpdateSessionRequest {
  title: string;
}

// 列表响应包装 (虽然 http 工具类已有 ApiResponse，但这里可以定义具体的业务响应类型方便引用)
export interface ChatSessionListResponse {
  sessions: ChatSession[];
}

export interface ChatMessageListResponse {
  messages: ChatMessage[];
}
