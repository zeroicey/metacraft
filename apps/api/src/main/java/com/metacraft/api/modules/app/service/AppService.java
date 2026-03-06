package com.metacraft.api.modules.app.service;

import com.metacraft.api.modules.app.dto.AppCreateDTO;
import com.metacraft.api.modules.app.dto.AppUpdateDTO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.app.vo.AppVO;
import com.metacraft.api.modules.app.vo.AppVersionVO;
import com.metacraft.api.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final StorageService storageService;

    /**
     * 创建新应用
     */
    @Transactional
    public AppEntity createApp(Long userId, String name, String description) {
        AppEntity app = AppEntity.builder()
                .userId(userId)
                .name(name)
                .uuid(java.util.UUID.randomUUID().toString())
                .description(description)
                .isPublic(false)
                .build();
        return appRepository.save(app);
    }

    /**
     * 创建新版本
     *
     * @param appId       应用ID
     * @param htmlContent HTML内容
     * @param changeLog   变更日志 (通常是用户的提示词)
     * @return 新版本实体
     */
    @Transactional
    public AppVersionEntity createVersion(Long appId, String htmlContent, String changeLog) {
        // 1. 获取应用
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App not found: " + appId));

        // 2. 计算新版本号
        Integer nextVersion = appVersionRepository.findTopByAppIdOrderByVersionNumberDesc(appId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        // 3. 保存文件
        String relativePath = String.format("apps/%d/v%d/index.html", appId, nextVersion);
        storageService.saveTextFile(relativePath, htmlContent);

        // 4. 创建版本记录
        AppVersionEntity version = AppVersionEntity.builder()
                .appId(appId)
                .versionNumber(nextVersion)
                .storagePath(relativePath)
                .changeLog(changeLog)
                .build();
        version = appVersionRepository.save(version);

        // 5. 更新应用当前版本指针
        app.setCurrentVersionId(version.getId());
        appRepository.save(app);

        log.info("Created version {} (id={}) for app {}", nextVersion, version.getId(), appId);
        return version;
    }
    
    /**
     * 获取应用
     */
    public AppEntity getApp(Long appId) {
        return appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App not found: " + appId));
    }

    /**
     * 更新应用元数据
     */
    @Transactional
    public void updateAppMetadata(Long appId, String name, String description) {
        appRepository.findById(appId).ifPresent(app -> {
            if (name != null && !name.isEmpty()) {
                app.setName(name);
            }
            if (description != null && !description.isEmpty()) {
                app.setDescription(description);
            }
            appRepository.save(app);
            log.info("Updated metadata for app {}: name={}, description={}", appId, name, description);
        });
    }

    /**
     * 更新应用 logo 文件名
     */
    @Transactional
    public void updateAppLogo(Long appId, String logo) {
        appRepository.findById(appId).ifPresent(app -> {
            app.setLogo(logo);
            appRepository.save(app);
            log.info("Updated logo for app {}: logo={}", appId, logo);
        });
    }

    /**
     * 获取用户最新创建的应用
     */
    public AppEntity getLatestAppByUserId(Long userId) {
        return appRepository.findTopByUserIdOrderByIdDesc(userId)
                .orElse(null);
    }

    /**
     * 根据用户和 logoUuid 获取本次生成的应用（logo 字段通常形如 uuid.png）。
     */
    public AppEntity getLatestAppByUserIdAndLogoUuid(Long userId, String logoUuid) {
        if (logoUuid == null || logoUuid.isBlank()) {
            return null;
        }
        return appRepository.findTopByUserIdAndLogoStartingWithOrderByIdDesc(userId, logoUuid)
                .orElse(null);
    }

    /**
     * 创建新应用 (使用 DTO)
     */
    @Transactional
    public AppVO createApp(Long userId, AppCreateDTO dto) {
        AppEntity app = AppEntity.builder()
                .userId(userId)
                .name(dto.getName())
                .uuid(java.util.UUID.randomUUID().toString())
                .description(dto.getDescription())
                .isPublic(false)
                .build();
        AppEntity saved = appRepository.save(app);
        log.info("Created new app with id={} and uuid={}", saved.getId(), saved.getUuid());
        return convertToVO(saved);
    }

    /**
     * 获取用户的所有应用（带版本信息）
     */
    public List<AppVO> getUserApps(Long userId) {
        List<AppEntity> apps = appRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return apps.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据 UUID 获取应用（带权限检查）
     */
    public AppVO getAppByUuid(Long userId, String uuid) {
        AppEntity app = appRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("App not found with uuid: " + uuid));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return convertToVO(app);
    }

    /**
     * 根据 ID 获取应用（带权限检查）
     */
    public AppVO getAppById(Long userId, Long appId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return convertToVO(app);
    }

    /**
     * 更新应用元数据
     */
    @Transactional
    public AppVO updateApp(Long userId, Long appId, AppUpdateDTO dto) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            app.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            app.setDescription(dto.getDescription());
        }
        if (dto.getIsPublic() != null) {
            app.setIsPublic(dto.getIsPublic());
        }

        AppEntity updated = appRepository.save(app);
        log.info("Updated app {} metadata", appId);
        return convertToVO(updated);
    }

    /**
     * 删除应用（包括所有版本和文件）
     */
    @Transactional
    public void deleteApp(Long userId, Long appId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        // 删除所有版本的文件
        List<AppVersionEntity> versions = appVersionRepository.findByAppIdOrderByVersionNumberDesc(appId);
        for (AppVersionEntity version : versions) {
            try {
                storageService.deleteFile(version.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete file for version {}: {}", version.getId(), e.getMessage());
            }
        }

        // 删除版本记录
        appVersionRepository.deleteAll(versions);

        // 删除应用
        appRepository.delete(app);

        log.info("Deleted app {} and all its versions", appId);
    }

    /**
     * 获取应用的所有版本
     */
    public List<AppVersionVO> getAppVersions(Long userId, Long appId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return appVersionRepository.findByAppIdOrderByVersionNumberDesc(appId).stream()
                .map(this::convertToVersionVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定版本
     */
    public AppVersionVO getVersion(Long userId, Long appId, Integer versionNumber) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return appVersionRepository.findByAppIdAndVersionNumber(appId, versionNumber)
                .map(this::convertToVersionVO)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionNumber));
    }

    /**
     * 删除指定版本（包括文件）
     */
    @Transactional
    public void deleteVersion(Long userId, Long appId, Long versionId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        AppVersionEntity version = appVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found with id: " + versionId));

        if (!version.getAppId().equals(appId)) {
            throw new IllegalArgumentException("Version does not belong to this app");
        }

        // 删除文件
        try {
            storageService.deleteFile(version.getStoragePath());
        } catch (Exception e) {
            log.warn("Failed to delete file for version {}: {}", version.getId(), e.getMessage());
        }

        // 如果删除的是当前版本，需要更新应用的 currentVersionId
        if (app.getCurrentVersionId() != null && app.getCurrentVersionId().equals(versionId)) {
            // 获取最新的其他版本
            List<AppVersionEntity> remainingVersions = appVersionRepository.findByAppIdOrderByVersionNumberDesc(appId)
                    .stream()
                    .filter(v -> !v.getId().equals(versionId))
                    .collect(Collectors.toList());

            if (!remainingVersions.isEmpty()) {
                app.setCurrentVersionId(remainingVersions.get(0).getId());
            } else {
                app.setCurrentVersionId(null);
            }
            appRepository.save(app);
        }

        // 删除版本记录
        appVersionRepository.delete(version);

        log.info("Deleted version {} of app {}", versionId, appId);
    }

    /**
     * 转换为 AppVO
     */
    private AppVO convertToVO(AppEntity entity) {
        List<AppVersionVO> versions = appVersionRepository.findByAppIdOrderByVersionNumberDesc(entity.getId())
                .stream()
                .map(this::convertToVersionVO)
                .collect(Collectors.toList());

        return AppVO.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
            .logo(entity.getLogo())
                .isPublic(entity.getIsPublic())
                .currentVersionId(entity.getCurrentVersionId())
                .versions(versions)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 转换为 AppVersionVO
     */
    private AppVersionVO convertToVersionVO(AppVersionEntity entity) {
        return AppVersionVO.builder()
                .id(entity.getId())
                .versionNumber(entity.getVersionNumber())
                .storagePath(entity.getStoragePath())
                .changeLog(entity.getChangeLog())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
