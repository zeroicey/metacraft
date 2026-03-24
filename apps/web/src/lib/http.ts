import ky from "ky";
import { API_PREFIX_URL } from "./config";

/**
 * API response wrapper
 * Corresponds to backend ApiResponse.java
 */
export interface ApiResponse<T = unknown> {
    message: string;
    data?: T;
    error?: unknown;
}

const http = ky.extend({
    prefixUrl: API_PREFIX_URL,
    timeout: 5000,
    hooks: {
        beforeRequest: [
            (request) => {
                const token = localStorage.getItem("token");
                if (token) {
                    request.headers.set("Authorization", `Bearer ${token}`);
                }
            }
        ],
        afterResponse: [
            (_request, _options, response) => {
                if (response.status === 401 || response.status === 403) {
                    // 清除本地存储的 token
                    localStorage.removeItem('token');
                    // 跳转到登录页
                    window.location.href = '/login';
                }
            },
        ],
    }
});

export default http;
