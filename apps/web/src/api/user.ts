import http, { type ApiResponse } from "@/lib/http";

export interface User {
  id: number;
  email: string;
  name: string;
  avatarBase64?: string;
  bio?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserUpdateRequest {
  name?: string;
  bio?: string;
  avatarBase64?: string;
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = async (): Promise<User> => {
  const response = await http.get("user").json<ApiResponse<User>>();
  if (!response.data) {
    throw new Error(response.message || "Failed to fetch user");
  }
  return response.data;
};

/**
 * 更新用户信息
 */
export const updateUser = async (data: UserUpdateRequest): Promise<User> => {
  const response = await http.patch("user", { json: data }).json<ApiResponse<User>>();
  if (!response.data) {
    throw new Error(response.message || "Failed to update user");
  }
  return response.data;
};