import { rcp } from '@kit.RemoteCommunicationKit';
import { BusinessError } from '@kit.BasicServicesKit';

// 请求拦截器：用于注入 Token 或公共 Header
export class AuthInterceptor implements rcp.Interceptor {
  async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
    // 假设 Token 存储在 AppStorage 或其他持久化存储中
    // const token = AppStorage.get<string>('token');
    // if (token) {
    //   context.request.headers['Authorization'] = `Bearer ${token}`;
    // }

    // 可以在这里添加其他公共 Header，如语言环境、版本号等
    // context.request.headers['Accept-Language'] = 'zh-CN';

    return next.handle(context);
  }
}

// 响应拦截器：用于处理全局错误码 (如 401)
export class ResponseInterceptor implements rcp.Interceptor {
  async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
    const response = await next.handle(context);

    // 统一处理 HTTP 状态码
    if (response.statusCode === 401) {
      // 处理 Token 过期，例如跳转登录页
      console.warn('[Network] Token expired (401)');
      // router.pushUrl({ url: 'pages/Login' });
      throw new Error('Unauthorized'); // 抛出错误中断业务逻辑
    } else if (response.statusCode >= 400) {
      console.error(`[Network] HTTP Error: ${response.statusCode}`);
      throw new Error(`HTTP Error ${response.statusCode}`);
    }

    return response;
  }
}