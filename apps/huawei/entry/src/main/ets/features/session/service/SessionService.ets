import { http, ApiResponse } from '../../../core/http/HttpManager';
import { ChatSession, ChatMessage } from '../domain/Session';
import { CreateSessionRequest, UpdateSessionRequest } from '../request/SessionRequests';
import { Result } from '../../../shared/types/Result';

class SessionService {
  async createSession(title: string): Promise<Result<ChatSession>> {
    console.info('[SessionService] Create session:', title);
    try {
      const req: CreateSessionRequest = { title };
      const response = await http.post<ApiResponse<ChatSession>>('/api/ai/sessions', req);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Create session failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[SessionService] Create session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getUserSessions(): Promise<Result<ChatSession[]>> {
    console.info('[SessionService] Get user sessions');
    try {
      const response = await http.get<ApiResponse<ChatSession[]>>('/api/ai/sessions');

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get sessions failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[SessionService] Get sessions error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getSession(sessionId: string): Promise<Result<ChatSession>> {
    console.info('[SessionService] Get session:', sessionId);
    try {
      const response = await http.get<ApiResponse<ChatSession>>(`/api/ai/sessions/${sessionId}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get session failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[SessionService] Get session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async updateSession(sessionId: string, title: string): Promise<Result<ChatSession>> {
    console.info('[SessionService] Update session:', sessionId, title);
    try {
      const req: UpdateSessionRequest = { title };
      const response = await http.patch<ApiResponse<ChatSession>>(`/api/ai/sessions/${sessionId}`, req);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Update session failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[SessionService] Update session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async deleteSession(sessionId: string): Promise<Result<void>> {
    console.info('[SessionService] Delete session:', sessionId);
    try {
      await http.delete<ApiResponse<void>>(`/api/ai/sessions/${sessionId}`);
      return { success: true };
    } catch (err) {
      const error = err as Error;
      console.error('[SessionService] Delete session error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getSessionMessages(sessionId: string): Promise<Result<ChatMessage[]>> {
    console.info('[SessionService] Get session messages:', sessionId);
    try {
      const response = await http.get<ApiResponse<ChatMessage[]>>(`/api/ai/sessions/${sessionId}/messages`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get messages failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[SessionService] Get messages error:', error.message);
      return { success: false, error: error.message };
    }
  }
}

export const sessionService = new SessionService();