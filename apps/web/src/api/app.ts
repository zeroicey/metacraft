import http, { type ApiResponse } from "@/lib/http";
import type { App, AppVersion, CreateAppRequest } from "@/types/app";

/**
 * App update request
 */
export interface UpdateAppRequest {
    name?: string;
    description?: string;
    isPublic?: boolean;
}

/**
 * Get all apps for the current user
 * @returns List of user apps
 */
export const getUserApps = async (): Promise<App[]> => {
    const response = await http
        .get("apps")
        .json<ApiResponse<App[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch apps");
    }
    return response.data;
};

/**
 * Get an app by UUID
 * @param uuid App UUID
 * @returns App details
 */
export const getAppByUuid = async (uuid: string): Promise<App> => {
    const response = await http
        .get(`apps/uuid/${uuid}`)
        .json<ApiResponse<App>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch app");
    }
    return response.data;
};

/**
 * Create a new app
 * @param data App creation data
 * @returns Created app
 */
export const createApp = async (data: CreateAppRequest): Promise<App> => {
    const response = await http
        .post("apps", { json: data })
        .json<ApiResponse<App>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to create app");
    }
    return response.data;
};

/**
 * Update an app
 * @param appId App ID
 * @param data App update data
 * @returns Updated app
 */
export const updateApp = async (
    appId: number,
    data: UpdateAppRequest
): Promise<App> => {
    const response = await http
        .patch(`apps/${appId}`, { json: data })
        .json<ApiResponse<App>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to update app");
    }
    return response.data;
};

/**
 * Delete an app
 * @param appId App ID
 */
export const deleteApp = async (appId: number): Promise<void> => {
    const response = await http
        .delete(`apps/${appId}`)
        .json<ApiResponse<void>>();
    if (response.error) {
        throw new Error(response.message || "Failed to delete app");
    }
};

/**
 * Get all versions of an app
 * @param appId App ID
 * @returns List of app versions
 */
export const getAppVersions = async (appId: number): Promise<AppVersion[]> => {
    const response = await http
        .get(`apps/${appId}/versions`)
        .json<ApiResponse<AppVersion[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch app versions");
    }
    return response.data;
};