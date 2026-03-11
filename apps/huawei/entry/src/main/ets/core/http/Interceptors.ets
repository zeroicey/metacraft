import { rcp } from '@kit.RemoteCommunicationKit';
import { tokenService } from '../../features/auth/model/AuthModel';

let onUnauthorizedCallback: (() => void) | null = null;

const AUTH_WHITELIST: string[] = ['/api/auth/login', '/api/auth/register'];

export function setOnUnauthorizedCallback(callback: () => void): void {
  onUnauthorizedCallback = callback;
}

export function clearOnUnauthorizedCallback(): void {
  onUnauthorizedCallback = null;
}

function isAuthWhitelisted(url: string): boolean {
  return AUTH_WHITELIST.some(path => url.includes(path));
}

export class AuthInterceptor implements rcp.Interceptor {
  async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
    const url = context.request.url?.toString() || '';

    if (isAuthWhitelisted(url)) {
      console.info('[AuthInterceptor] 跳过token注入 (白名单接口):', url);
      return next.handle(context);
    }

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

export class ResponseInterceptor implements rcp.Interceptor {
  async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
    const url = context.request.url?.toString() || '';
    const response = await next.handle(context);

    console.info('[ResponseInterceptor] URL:', url);
    console.info('[ResponseInterceptor] 状态码:', response.statusCode);

    if (response.statusCode === 401 || response.statusCode === 403) {
      console.warn(`[ResponseInterceptor] Unauthorized (${response.statusCode})`);

      if (isAuthWhitelisted(url)) {
        console.info('[ResponseInterceptor] 白名单接口，返回原始响应');
        return response;
      }

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