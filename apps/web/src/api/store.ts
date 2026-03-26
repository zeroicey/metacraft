import http, { type ApiResponse } from "@/lib/http";

/**
 * Store app item for list view
 */
export interface StoreAppItem {
    id: number;
    uuid: string;
    name: string;
    description: string;
    logo: string | null;
    averageRating: number | null;
    ratingCount: number;
    author: {
        id: number;
        name: string;
        avatarBase64: string | null;
    };
    createdAt: string;
}

/**
 * Comment item
 */
export interface StoreComment {
    id: number;
    userId: number;
    userName: string;
    userAvatar: string | null;
    content: string;
    createdAt: string;
}

/**
 * Store app detail with comments
 */
export interface StoreAppDetail extends StoreAppItem {
    comments: StoreComment[];
}

/**
 * Rating result
 */
export interface RatingResult {
    averageRating: number;
    ratingCount: number;
}

/**
 * Get store apps list
 * @returns List of published apps in the store
 */
export const getStoreApps = async (): Promise<StoreAppItem[]> => {
    const response = await http.get("store/apps").json<ApiResponse<StoreAppItem[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch store apps");
    }
    return response.data;
};

/**
 * Get app detail by ID
 * @param appId App ID
 * @returns App detail with comments
 */
export const getStoreAppDetail = async (appId: number): Promise<StoreAppDetail> => {
    const response = await http.get(`store/apps/${appId}`).json<ApiResponse<StoreAppDetail>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch app detail");
    }
    return response.data;
};

/**
 * Submit rating for an app
 * @param appId App ID
 * @param rating Rating value (1-5)
 * @returns Updated rating info
 */
export const rateApp = async (appId: number, rating: number): Promise<RatingResult> => {
    const response = await http
        .post(`store/apps/${appId}/ratings`, { json: { rating } })
        .json<ApiResponse<RatingResult>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to submit rating");
    }
    return response.data;
};

/**
 * Submit comment for an app
 * @param appId App ID
 * @param content Comment content
 * @returns Created comment
 */
export const commentApp = async (appId: number, content: string): Promise<StoreComment> => {
    const response = await http
        .post(`store/apps/${appId}/comments`, { json: { content } })
        .json<ApiResponse<StoreComment>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to submit comment");
    }
    return response.data;
};

/**
 * Delete a comment
 * @param appId App ID
 * @param commentId Comment ID
 */
export const deleteComment = async (appId: number, commentId: number): Promise<void> => {
    const response = await http
        .delete(`store/apps/${appId}/comments/${commentId}`)
        .json<ApiResponse<void>>();
    if (response.message && !response.data) {
        throw new Error(response.message);
    }
};

/**
 * Publish an app to the store
 * @param appId App ID
 */
export const publishApp = async (appId: number): Promise<void> => {
    const response = await http
        .post(`store/apps/${appId}/publish`)
        .json<ApiResponse<void>>();
    if (response.message && !response.data) {
        throw new Error(response.message);
    }
};

/**
 * Unpublish an app from the store
 * @param appId App ID
 */
export const unpublishApp = async (appId: number): Promise<void> => {
    const response = await http
        .delete(`store/apps/${appId}/publish`)
        .json<ApiResponse<void>>();
    if (response.message && !response.data) {
        throw new Error(response.message);
    }
};