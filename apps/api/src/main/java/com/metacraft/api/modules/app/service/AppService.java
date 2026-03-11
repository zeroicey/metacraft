package com.metacraft.api.modules.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.dto.AppCodeSnapshotDTO;
import com.metacraft.api.modules.app.dto.AppCreateDTO;
import com.metacraft.api.modules.app.dto.AppUpdateDTO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.vo.AppVO;
import com.metacraft.api.modules.app.vo.AppVersionVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppService {

    private final AppApplicationService appApplicationService;
    private final AppVersionService appVersionService;
    private final AppQueryService appQueryService;

    /**
     * 创建新应用
     */
    public AppEntity createApp(Long userId, String name, String description) {
        return appApplicationService.createApp(userId, name, description);
    }

    /**
     * 创建新版本
     *
     * @param appId       应用ID
     * @param htmlContent HTML内容
    * @param jsContent   JavaScript内容 (app.js)
     * @param changeLog   变更日志 (通常是用户的提示词)
     * @return 新版本实体
     */
    public AppVersionEntity createVersion(Long appId, String htmlContent, String jsContent, String changeLog) {
        return appVersionService.createVersion(appId, htmlContent, jsContent, changeLog);
    }
    
    /**
     * 获取应用
     */
    public AppEntity getApp(Long appId) {
        return appApplicationService.getApp(appId);
    }

    /**
     * 更新应用元数据
     */
    public void updateAppMetadata(Long appId, String name, String description) {
        appApplicationService.updateAppMetadata(appId, name, description);
    }

    /**
     * 更新应用 logo 文件名
     */
    public void updateAppLogo(Long appId, String logo) {
        appApplicationService.updateAppLogo(appId, logo);
    }

    /**
     * 获取用户最新创建的应用
     */
    public AppEntity getLatestAppByUserId(Long userId) {
        return appApplicationService.getLatestAppByUserId(userId);
    }

    /**
     * 根据用户和 logoUuid 获取本次生成的应用（logo 字段通常形如 uuid.png）。
     */
    public AppEntity getLatestAppByUserIdAndLogoUuid(Long userId, String logoUuid) {
        return appApplicationService.getLatestAppByUserIdAndLogoUuid(userId, logoUuid);
    }

    /**
     * 创建新应用 (使用 DTO)
     */
    public AppVO createApp(Long userId, AppCreateDTO dto) {
        AppEntity created = appApplicationService.createApp(userId, dto);
        return appQueryService.getAppById(userId, created.getId());
    }

    /**
     * 获取用户的所有应用（带版本信息）
     */
    public List<AppVO> getUserApps(Long userId) {
        return appQueryService.getUserApps(userId);
    }

    /**
     * 根据 UUID 获取应用（带权限检查）
     */
    public AppVO getAppByUuid(Long userId, String uuid) {
        return appQueryService.getAppByUuid(userId, uuid);
    }

    /**
     * 根据 ID 获取应用（带权限检查）
     */
    public AppVO getAppById(Long userId, Long appId) {
        return appQueryService.getAppById(userId, appId);
    }

    public AppCodeSnapshotDTO getCurrentCode(Long userId, Long appId) {
        return appQueryService.getCurrentCode(userId, appId);
    }

    /**
     * 更新应用元数据
     */
    public AppVO updateApp(Long userId, Long appId, AppUpdateDTO dto) {
        AppEntity updated = appApplicationService.updateApp(userId, appId, dto);
        return appQueryService.getAppById(userId, updated.getId());
    }

    /**
     * 删除应用（包括所有版本和文件）
     */
    public void deleteApp(Long userId, Long appId) {
        appApplicationService.deleteApp(userId, appId);
    }

    /**
     * 获取应用的所有版本
     */
    public List<AppVersionVO> getAppVersions(Long userId, Long appId) {
        return appQueryService.getAppVersions(userId, appId);
    }

    /**
     * 获取指定版本
     */
    public AppVersionVO getVersion(Long userId, Long appId, Integer versionNumber) {
        return appQueryService.getVersion(userId, appId, versionNumber);
    }

    /**
     * 删除指定版本（包括文件）
     */
    public void deleteVersion(Long userId, Long appId, Long versionId) {
        appVersionService.deleteVersion(userId, appId, versionId);
    }
}
