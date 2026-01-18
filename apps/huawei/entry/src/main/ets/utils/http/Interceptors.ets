import { rcp } from '@kit.RemoteCommunicationKit';
import { tokenService } from '../../model/auth';

// 全局登录回调，用于在401/403时触发登录弹窗
let onUnauthorizedCallback: (() => void) | null = null;

// 不需要触发重新登录的接口路径
const AUTH_WHITELIST: string[] = ['/api/auth/login', '/api/auth/register'];

export function setOnUnauthorizedCallback(callback: () => void): void {
  onUnauthorizedCallback = callback;
}

export function clearOnUnauthorizedCallback(): void {
  onUnauthorizedCallback = null;
}

// 检查URL是否在白名单中
function isAuthWhitelisted(url: string): boolean {
  return AUTH_WHITELIST.some(path => url.includes(path));
}

// 请求拦截器：用于注入 Token
export class AuthInterceptor implements rcp.Interceptor {
  async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
    const url = context.request.url?.toString() || '';
    
    // 登录相关接口不需要携带token
    if (isAuthWhitelisted(url)) {
      console.info('[AuthInterceptor] 跳过token注入 (白名单接口):', url);
      return next.handle(context);
    }
    
    // 从TokenService获取token
    const token = tokenService.getToken();
    if (token) {
      const tokenType = tokenService.getTokenType();
      context.request.headers = context.request.headers || {};
      context.request.headers['Authorization'] = `${tokenType} ${token}`;
      console.info('[AuthInterceptor] Token attached to request');
    }

    return next.handle(context);
  }
}

// 响应拦截器：用于处理全局错误码 (如 401, 403)
export class ResponseInterceptor implements rcp.Interceptor {
  async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
    const url = context.request.url?.toString() || '';
    const response = await next.handle(context);

    console.info('[ResponseInterceptor] URL:', url);
    console.info('[ResponseInterceptor] 状态码:', response.statusCode);

    // 统一处理 HTTP 状态码
    if (response.statusCode === 401 || response.statusCode === 403) {
      console.warn(`[ResponseInterceptor] Unauthorized (${response.statusCode})`);
      
      // 登录相关接口的401/403不触发重新登录，直接返回响应让业务层处理
      if (isAuthWhitelisted(url)) {
        console.info('[ResponseInterceptor] 白名单接口，返回原始响应');
        return response;
      }
      
      // 非登录接口的401/403，清除token并触发登录弹窗
      console.info('[ResponseInterceptor] 清除token并触发登录');
      tokenService.clearToken();
      
      if (onUnauthorizedCallback) {
        onUnauthorizedCallback();
      }
      
      throw new Error('Unauthorized');
    } else if (response.statusCode >= 400) {
      console.error(`[ResponseInterceptor] HTTP Error: ${response.statusCode}`);
      throw new Error(`HTTP Error ${response.statusCode}`);
    }

    return response;
  }
}
