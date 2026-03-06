/**
 * App Data Models
 */

// 应用版本信息 VO
export interface AppVersion {
  id: number;
  versionNumber: number;
  storagePath: string;
  changeLog: string;
  createdAt: string; // ISO 8601 string
}

// 应用信息 VO
export interface App {
  id: number;
  uuid: string;
  name: string;
  description: string;
  logo?: string;
  isPublic: boolean;
  currentVersionId: number | null;
  versions: AppVersion[];
  createdAt: string; // ISO 8601 string
  updatedAt: string; // ISO 8601 string
}

// 创建应用请求 DTO
export interface CreateAppRequest {
  name: string;
  description: string;
}

// 更新应用请求 DTO
export interface UpdateAppRequest {
  name?: string;
  description?: string;
  isPublic?: boolean;
}

// 结果包装接口
export interface Result<T> {
  success: boolean;
  data?: T;
  error?: string;
}
