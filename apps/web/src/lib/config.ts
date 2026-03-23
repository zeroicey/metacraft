/**
 * API 配置
 * 所有 API 相关的常量统一在这里管理
 */

const DEFAULT_API_BASE_URL = "http://localhost:8080"

/**
 * localStorage 中存储自定义后端地址的 key
 */
const CUSTOM_API_URL_KEY = "customApiUrl"

/**
 * 获取当前配置的基础地址
 * 优先使用 localStorage 中用户自定义的后端地址
 */
function getCurrentBaseUrl(): string {
    const customUrl = localStorage.getItem(CUSTOM_API_URL_KEY);
    if (customUrl && customUrl.trim()) {
        const trimmed = customUrl.trim();
        return trimmed.startsWith("http") ? trimmed : `http://${trimmed}`;
    }
    return DEFAULT_API_BASE_URL;
}

/**
 * 设置自定义后端地址
 */
export function setCustomApiUrl(url: string): void {
    localStorage.setItem(CUSTOM_API_URL_KEY, url);
}

/**
 * 获取当前自定义后端地址
 */
export function getCustomApiUrl(): string | null {
    return localStorage.getItem(CUSTOM_API_URL_KEY);
}

/**
 * 清除自定义后端地址
 */
export function clearCustomApiUrl(): void {
    localStorage.removeItem(CUSTOM_API_URL_KEY);
}

/**
 * API 基础地址（不含 /api 后缀）
 * 支持动态获取自定义地址
 */
export const API_BASE_URL = getCurrentBaseUrl()

/**
 * API 前缀地址（包含 /api 后缀）
 */
export const API_PREFIX_URL = `${API_BASE_URL}/api`