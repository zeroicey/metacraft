import { http, ApiResponse } from '../utils/http/HttpManager';
import {
  App,
  AppVersion,
  CreateAppRequest,
  UpdateAppRequest,
  Result
} from '../model/appModel';

class AppApi {

  /**
   * 获取当前用户的所有应用
   * GET /api/apps
   */
  async getUserApps(): Promise<Result<App[]>> {
    console.info('[AppApi] Get user apps');
    try {
      const response = await http.get<ApiResponse<App[]>>('/api/apps');

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get apps failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Get apps error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 根据 ID 获取应用详情
   * GET /api/apps/{appId}
   */
  async getAppById(appId: number): Promise<Result<App>> {
    console.info('[AppApi] Get app by id:', appId);
    try {
      const response = await http.get<ApiResponse<App>>(`/api/apps/${appId}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Get app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 根据 UUID 获取应用详情
   * GET /api/apps/uuid/{uuid}
   */
  async getAppByUuid(uuid: string): Promise<Result<App>> {
    console.info('[AppApi] Get app by uuid:', uuid);
    try {
      const response = await http.get<ApiResponse<App>>(`/api/apps/uuid/${uuid}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Get app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 创建新应用
   * POST /api/apps
   */
  async createApp(req: CreateAppRequest): Promise<Result<App>> {
    console.info('[AppApi] Create app:', req.name);
    try {
      const response = await http.post<ApiResponse<App>>('/api/apps', req);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Create app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Create app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 更新应用信息
   * PATCH /api/apps/{appId}
   */
  async updateApp(appId: number, req: UpdateAppRequest): Promise<Result<App>> {
    console.info('[AppApi] Update app:', appId, req);
    try {
      const response = await http.patch<ApiResponse<App>>(`/api/apps/${appId}`, req);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Update app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Update app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 删除应用（包括所有版本和文件）
   * DELETE /api/apps/{appId}
   */
  async deleteApp(appId: number): Promise<Result<void>> {
    console.info('[AppApi] Delete app:', appId);
    try {
      const response = await http.delete<ApiResponse<void>>(`/api/apps/${appId}`);
      return { success: true };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Delete app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 获取应用的所有版本
   * GET /api/apps/{appId}/versions
   */
  async getAppVersions(appId: number): Promise<Result<AppVersion[]>> {
    console.info('[AppApi] Get app versions:', appId);
    try {
      const response = await http.get<ApiResponse<AppVersion[]>>(`/api/apps/${appId}/versions`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get versions failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Get versions error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 获取指定版本的详情
   * GET /api/apps/{appId}/versions/{versionNumber}
   */
  async getVersion(appId: number, versionNumber: number): Promise<Result<AppVersion>> {
    console.info('[AppApi] Get version:', appId, versionNumber);
    try {
      const response = await http.get<ApiResponse<AppVersion>>(`/api/apps/${appId}/versions/${versionNumber}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get version failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Get version error:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 删除指定版本（包括文件）
   * DELETE /api/apps/{appId}/versions/{versionId}
   */
  async deleteVersion(appId: number, versionId: number): Promise<Result<void>> {
    console.info('[AppApi] Delete version:', appId, versionId);
    try {
      const response = await http.delete<ApiResponse<void>>(`/api/apps/${appId}/versions/${versionId}`);
      return { success: true };
    } catch (err) {
      const error = err as Error;
      console.error('[AppApi] Delete version error:', error.message);
      return { success: false, error: error.message };
    }
  }
}

export const appApi = new AppApi();
