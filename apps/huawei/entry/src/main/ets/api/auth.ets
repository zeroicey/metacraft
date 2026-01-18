import { http, ApiResponse } from '../utils/http/HttpManager';
import { LoginRequest, LoginResponseData, tokenService } from '../model/auth';

/**
 * 认证相关API
 * 
 * @使用示例:
 * ```typescript
 * import { authApi } from '../api/auth';
 * 
 * // 登录
 * const result = await authApi.login();
 * if (result.success) {
 *   console.info('登录成功，token已保存');
 * } else {
 *   console.error('登录失败:', result.error);
 * }
 * ```
 */

// 登录结果
export interface LoginResult {
  success: boolean;
  error?: string;
}

class AuthApi {
  /**
   * 使用固定凭据登录后端
   * 登录成功后自动保存token到本地存储
   */
  async login(): Promise<LoginResult> {
    console.info('[AuthApi] ========== 开始登录 ==========');
    
    const loginData: LoginRequest = {
      email: 'user@metacraft.com',
      password: 'testuser'
    };
    
    console.info('[AuthApi] 请求URL: /auth/login');
    console.info('[AuthApi] 请求数据:', JSON.stringify(loginData));
    
    try {
      const response = await http.post<ApiResponse<LoginResponseData>>('/api/auth/login', loginData);
      
      console.info('[AuthApi] 响应状态:', response.status);
      console.info('[AuthApi] 响应消息:', response.message);
      console.info('[AuthApi] 响应数据:', JSON.stringify(response.data));
      console.info('[AuthApi] 响应错误:', response.error);
      
      if (response.data && response.data.token) {
        console.info('[AuthApi] 登录成功，开始保存token');
        console.info('[AuthApi] Token类型:', response.data.type);
        console.info('[AuthApi] Token过期时间:', response.data.expiresIn);
        console.info('[AuthApi] Token值:', response.data.token.substring(0, 20) + '...');
        
        tokenService.saveToken(response.data);
        
        console.info('[AuthApi] Token保存完成');
        console.info('[AuthApi] ========== 登录成功 ==========');
        
        return { success: true };
      } else {
        const errorMsg = response.message || response.error || '未知错误';
        console.error('[AuthApi] 登录失败，响应中无有效token');
        console.error('[AuthApi] 错误信息:', errorMsg);
        console.info('[AuthApi] ========== 登录失败 ==========');
        
        return { success: false, error: errorMsg };
      }
    } catch (err) {
      const error = err as Error;
      console.error('[AuthApi] 登录请求异常');
      console.error('[AuthApi] 错误类型:', typeof err);
      console.error('[AuthApi] 错误消息:', error.message);
      console.error('[AuthApi] 错误堆栈:', error.stack);
      console.info('[AuthApi] ========== 登录异常 ==========');
      
      return { success: false, error: error.message || '网络请求失败' };
    }
  }
  
  /**
   * 退出登录
   * 清除本地存储的token
   */
  logout(): void {
    console.info('[AuthApi] ========== 开始退出登录 ==========');
    tokenService.clearToken();
    console.info('[AuthApi] Token已清除');
    console.info('[AuthApi] ========== 退出登录完成 ==========');
  }
  
  /**
   * 检查是否已登录
   */
  isLoggedIn(): boolean {
    const hasToken = tokenService.hasToken();
    console.info('[AuthApi] 检查登录状态:', hasToken ? '已登录' : '未登录');
    return hasToken;
  }
}

export const authApi = new AuthApi();
