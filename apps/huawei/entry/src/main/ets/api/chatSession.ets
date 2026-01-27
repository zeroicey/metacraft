import { http, ApiResponse } from '../utils/http/HttpManager';
import { 
  ChatSession, 
  ChatMessage, 
  CreateSessionRequest, 
  UpdateSessionRequest 
} from '../model/chatSessionModel';

/**
 * 结果包装接口
 */
export interface Result<T> {
  success: boolean;
  data?: T;
  error?: string;
}

class ChatSessionApi {
  
  /**
   * 创建新会话
   * POST /ai/sessions
   */
  async createSession(title: string): Promise<Result<ChatSession>> {
    console.info('[ChatSessionApi] Create session:', title);
    try {
      const req: CreateSessionRequest = { title };
      const response = await http.post<ApiResponse<ChatSession>>('/api/ai/sessions', req);
      
      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Create session failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[ChatSessionApi] Create session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 获取当前用户的所有会话
   * GET /ai/sessions
   */
  async getUserSessions(): Promise<Result<ChatSession[]>> {
    console.info('[ChatSessionApi] Get user sessions');
    try {
      const response = await http.get<ApiResponse<ChatSession[]>>('/api/ai/sessions');
      
      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get sessions failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[ChatSessionApi] Get sessions error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 获取指定会话详情
   * GET /ai/sessions/{sessionId}
   */
  async getSession(sessionId: string): Promise<Result<ChatSession>> {
    console.info('[ChatSessionApi] Get session:', sessionId);
    try {
      const response = await http.get<ApiResponse<ChatSession>>(`/api/ai/sessions/${sessionId}`);
      
      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get session failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[ChatSessionApi] Get session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 更新会话
   * PATCH /ai/sessions/{sessionId}
   */
  async updateSession(sessionId: string, title: string): Promise<Result<ChatSession>> {
    console.info('[ChatSessionApi] Update session:', sessionId, title);
    try {
      const req: UpdateSessionRequest = { title };
      const response = await http.patch<ApiResponse<ChatSession>>(`/api/ai/sessions/${sessionId}`, req);
      
      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Update session failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[ChatSessionApi] Update session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 删除会话
   * DELETE /ai/sessions/{sessionId}
   */
  async deleteSession(sessionId: string): Promise<Result<void>> {
    console.info('[ChatSessionApi] Delete session:', sessionId);
    try {
      const response = await http.delete<ApiResponse<void>>(`/api/ai/sessions/${sessionId}`);
      
      // 200-299 都是成功
      return { success: true };
    } catch (err) {
      const error = err as Error;
      console.error('[ChatSessionApi] Delete session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 获取会话的所有消息
   * GET /ai/sessions/{sessionId}/messages
   */
  async getSessionMessages(sessionId: string): Promise<Result<ChatMessage[]>> {
    console.info('[ChatSessionApi] Get session messages:', sessionId);
    try {
      const response = await http.get<ApiResponse<ChatMessage[]>>(`/api/ai/sessions/${sessionId}/messages`);
      
      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get messages failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[ChatSessionApi] Get messages error:', error.message);
      return { success: false, error: error.message };
    }
  }
}

export const chatSessionApi = new ChatSessionApi();
