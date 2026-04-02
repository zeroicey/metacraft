import http, { type ApiResponse } from "@/lib/http";
import type {
    LoginRequest,
    RegisterRequest,
    AuthTokenResponse,
    User,
} from "@/types/auth";

/**
 * User login API
 * @param data Login credentials
 * @returns Authentication token
 */
export const login = async (data: LoginRequest): Promise<AuthTokenResponse> => {
    const response = await http
        .post("auth/login", { json: data })
        .json<ApiResponse<AuthTokenResponse>>();
    if (!response.data) {
        throw new Error(response.message || "Login failed");
    }
    return response.data;
};

/**
 * User registration API
 * @param data Registration information
 * @returns Authentication token
 */
export const register = async (
    data: RegisterRequest
): Promise<AuthTokenResponse> => {
    const response = await http
        .post("auth/register", { json: data })
        .json<ApiResponse<AuthTokenResponse>>();
    if (!response.data) {
        throw new Error(response.message || "Registration failed");
    }
    return response.data;
};

/**
 * Get current user info via /user
 */

export const getCurrentUser = async (): Promise<User> => {
    const response = await http.get("user").json<ApiResponse<User>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch user");
    }
    return response.data;
};
