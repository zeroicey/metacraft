import { http, ApiResponse } from '../../../core/http/HttpManager';
import { tokenService, LoginRequest, LoginResponseData } from '../model/AuthModel';
import { Result } from '../../../shared/types/Result';

class AuthService {
  async login(): Promise<Result<void>> {
    console.info('[AuthService] ========== 开始登录 ==========');

    const loginData: LoginRequest = {
      email: 'user@metacraft.com',
      password: 'testuser'
    };

    try {
      const response = await http.post<ApiResponse<LoginResponseData>>('/api/auth/login', loginData);

      if (response.data && response.data.token) {
        tokenService.saveToken(response.data);
        console.info('[AuthService] ========== 登录成功 ==========');
        return { success: true };
      }

      const errorMsg = response.message || response.error || '未知错误';
      console.error('[AuthService] 登录失败:', errorMsg);
      return { success: false, error: errorMsg };
    } catch (err) {
      const error = err as Error;
      console.error('[AuthService] 登录异常:', error.message);
      return { success: false, error: error.message || '网络请求失败' };
    }
  }

  logout(): void {
    tokenService.clearToken();
  }

  isLoggedIn(): boolean {
    return tokenService.hasToken();
  }
}

export const authService = new AuthService();