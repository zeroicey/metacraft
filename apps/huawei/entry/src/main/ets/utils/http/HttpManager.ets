import { rcp } from '@kit.RemoteCommunicationKit';
import { AuthInterceptor, ResponseInterceptor } from './Interceptors';
import { BusinessError } from '@kit.BasicServicesKit';

// 定义通用的响应结构（根据后端实际返回定义）
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export class HttpManager {
  private static instance: HttpManager;
  private session: rcp.Session;
  private readonly baseUrl: string = 'https://api.example.com'; // 替换为你的 Base URL

  private constructor() {
    // 1. 配置 Session [4]
    const sessionConfig: rcp.SessionConfiguration = {
      baseAddress: this.baseUrl,
      headers: {
        'Content-Type': 'application/json'
      },
      // 2. 挂载拦截器 [2]
      interceptors: [
        new AuthInterceptor(),
        new ResponseInterceptor()
      ],
      // 可选：配置超时、代理等 [5][6]
      requestConfiguration: {
        transfer: {
          timeout: {
            connectMs: 5000,
            transferMs: 10000
          }
        }
      }
    };

    // 3. 创建 Session [7]
    try {
      this.session = rcp.createSession(sessionConfig);
    } catch (error) {
      const err = error as BusinessError;
      console.error(`[HttpManager] Failed to create session: ${err.code} - ${err.message}`);
      throw new Error(`Failed to create session: ${err.message}`);
    }
  }

  public static getInstance(): HttpManager {
    if (!HttpManager.instance) {
      HttpManager.instance = new HttpManager();
    }
    return HttpManager.instance;
  }

  // --- 核心请求方法 ---

  /**
   * 发起 GET 请求 [8]
   */
  public async get<T>(url: string, params?: Record<string, string | number>): Promise<T> {
    // 处理 Query 参数
    const fullUrl = this.buildUrl(url, params);
    try {
      const response = await this.session.get(fullUrl);
      return this.handleResponse<T>(response);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * 发起 POST 请求 [9]
   */
  public async post<T>(url: string, data?: object): Promise<T> {
    try {
      // 构造请求内容
      const content: rcp.RequestContent = data ? data : {};
      const response = await this.session.post(url, content);
      return this.handleResponse<T>(response);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * 发起 PUT 请求 [10]
   */
  public async put<T>(url: string, data?: object): Promise<T> {
    try {
      const content: rcp.RequestContent = data ? data : {};
      const response = await this.session.put(url, content);
      return this.handleResponse<T>(response);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  /**
   * 发起 DELETE 请求 [11]
   */
  public async delete<T>(url: string): Promise<T> {
    try {
      const response = await this.session.delete(url);
      return this.handleResponse<T>(response);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // --- 辅助方法 ---

  // 处理响应数据转换
  private handleResponse<T>(response: rcp.Response): T {
    if (response.statusCode !== 200) {
      throw new Error(`Request failed with status ${response.statusCode}`);
    }
    // 假设返回的是 JSON
    return response.toJSON() as T;
  }

  // 统一错误处理
  private handleError(error: BusinessError): Error {
    const err = error as BusinessError;
    console.error(`[Network] Request Error: ${err.code} - ${err.message}`);
    return new Error(err.message || 'Network Unknown Error');
  }

  // 拼接 Query 参数
  private buildUrl(url: string, params?: Record<string, string | number>): string {
    if (!params) return url;
    const query = Object.keys(params)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(String(params[key]))}`)
      .join('&');
    return `${url}?${query}`;
  }

  // 销毁 Session [15]
  public destroy(): void {
    if (this.session) {
      this.session.close();
    }
  }
}

export const http = HttpManager.getInstance();