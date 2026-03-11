import { http, ApiResponse } from '../../../core/http/HttpManager';
import { App, AppVersion } from '../domain/App';
import { CreateAppRequest, UpdateAppRequest } from '../request/AppRequests';
import { Result } from '../../../shared/types/Result';

class AppService {
  async getUserApps(): Promise<Result<App[]>> {
    console.info('[AppService] Get user apps');
    try {
      const response = await http.get<ApiResponse<App[]>>('/api/apps');

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get apps failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Get apps error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getAppById(appId: number): Promise<Result<App>> {
    console.info('[AppService] Get app by id:', appId);
    try {
      const response = await http.get<ApiResponse<App>>(`/api/apps/${appId}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Get app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getAppByUuid(uuid: string): Promise<Result<App>> {
    console.info('[AppService] Get app by uuid:', uuid);
    try {
      const response = await http.get<ApiResponse<App>>(`/api/apps/uuid/${uuid}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Get app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async createApp(req: CreateAppRequest): Promise<Result<App>> {
    console.info('[AppService] Create app:', req.name);
    try {
      const response = await http.post<ApiResponse<App>>('/api/apps', req);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Create app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Create app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async updateApp(appId: number, req: UpdateAppRequest): Promise<Result<App>> {
    console.info('[AppService] Update app:', appId, req);
    try {
      const response = await http.patch<ApiResponse<App>>(`/api/apps/${appId}`, req);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Update app failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Update app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async deleteApp(appId: number): Promise<Result<void>> {
    console.info('[AppService] Delete app:', appId);
    try {
      await http.delete<ApiResponse<void>>(`/api/apps/${appId}`);
      return { success: true };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Delete app error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getAppVersions(appId: number): Promise<Result<AppVersion[]>> {
    console.info('[AppService] Get app versions:', appId);
    try {
      const response = await http.get<ApiResponse<AppVersion[]>>(`/api/apps/${appId}/versions`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get versions failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Get versions error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async getVersion(appId: number, versionNumber: number): Promise<Result<AppVersion>> {
    console.info('[AppService] Get version:', appId, versionNumber);
    try {
      const response = await http.get<ApiResponse<AppVersion>>(`/api/apps/${appId}/versions/${versionNumber}`);

      if (response.data) {
        return { success: true, data: response.data };
      }
      return { success: false, error: response.message || 'Get version failed' };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Get version error:', error.message);
      return { success: false, error: error.message };
    }
  }

  async deleteVersion(appId: number, versionId: number): Promise<Result<void>> {
    console.info('[AppService] Delete version:', appId, versionId);
    try {
      await http.delete<ApiResponse<void>>(`/api/apps/${appId}/versions/${versionId}`);
      return { success: true };
    } catch (err) {
      const error = err as Error;
      console.error('[AppService] Delete version error:', error.message);
      return { success: false, error: error.message };
    }
  }
}

export const appService = new AppService();