import http from "ky";
import { useAuthStore } from "@/stores/auth-store";
import {
    API_PREFIX_URL,
} from "./config";

/**
 * API response wrapper
 * Corresponds to backend ApiResponse.java
 */
export interface ApiResponse<T = unknown> {
    message: string;
    data?: T;
    error?: unknown;
}

const httpInstance = http.extend({
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
                    const openLoginDrawer = useAuthStore.getState().openLoginDrawer;
                    openLoginDrawer();
                }
            },
        ],
    }
});

export default httpInstance;
