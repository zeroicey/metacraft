import { rcp } from '@kit.RemoteCommunicationKit';
import { AuthInterceptor, ResponseInterceptor } from './Interceptors';
import { BusinessError } from '@kit.BasicServicesKit';
import { API_BASE_URL } from './ApiConfig';

export interface ApiResponse<T> {
	message: string;
	status: number;
	data: T | null;
	error: string | null;
}

export class HttpManager {
	private static instance: HttpManager;
	private session: rcp.Session;
	private readonly baseUrl: string = API_BASE_URL;

	private constructor() {
		const sessionConfig: rcp.SessionConfiguration = {
			baseAddress: this.baseUrl,
			headers: {
				'Content-Type': 'application/json'
			},
			interceptors: [
				new AuthInterceptor(),
				new ResponseInterceptor()
			],
			requestConfiguration: {
				transfer: {
					timeout: {
						connectMs: 60000,
						transferMs: 3600000
					}
				}
			}
		};

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

	public async get<T>(url: string, params?: Record<string, string | number>): Promise<T> {
		const fullUrl = this.buildUrl(url, params);
		try {
			const response = await this.session.get(fullUrl);
			return this.handleResponse<T>(response);
		} catch (error) {
			throw this.handleError(error as BusinessError);
		}
	}

	public async post<T>(url: string, data?: object): Promise<T> {
		try {
			const content: rcp.RequestContent = data ? data : {};
			const response = await this.session.post(url, content);
			return this.handleResponse<T>(response);
		} catch (error) {
			throw this.handleError(error as BusinessError);
		}
	}

	public async put<T>(url: string, data?: object): Promise<T> {
		try {
			const content: rcp.RequestContent = data ? data : {};
			const response = await this.session.put(url, content);
			return this.handleResponse<T>(response);
		} catch (error) {
			throw this.handleError(error as BusinessError);
		}
	}

	public async patch<T>(url: string, data?: object): Promise<T> {
		try {
			const content: rcp.RequestContent = data ? data : {};
			const req = new rcp.Request(url, 'PATCH');
			req.content = content;
			const response = await this.session.fetch(req);
			return this.handleResponse<T>(response);
		} catch (error) {
			throw this.handleError(error as BusinessError);
		}
	}

	public async delete<T>(url: string): Promise<T> {
		try {
			const response = await this.session.delete(url);
			return this.handleResponse<T>(response);
		} catch (error) {
			throw this.handleError(error as BusinessError);
		}
	}

	private handleResponse<T>(response: rcp.Response): T {
		if (response.statusCode < 200 || response.statusCode >= 300) {
			throw new Error(`Request failed with status ${response.statusCode}`);
		}
		return response.toJSON() as T;
	}

	private handleError(error: BusinessError): Error {
		const err = error as BusinessError;
		console.error(`[Network] Request Error: ${err.code} - ${err.message}`);
		return new Error(err.message || 'Network Unknown Error');
	}

	private buildUrl(url: string, params?: Record<string, string | number>): string {
		if (!params) return url;
		const query = Object.keys(params)
			.map(key => `${encodeURIComponent(key)}=${encodeURIComponent(String(params[key]))}`)
			.join('&');
		return `${url}?${query}`;
	}

	public destroy(): void {
		if (this.session) {
			this.session.close();
		}
	}
}

export const http = HttpManager.getInstance();