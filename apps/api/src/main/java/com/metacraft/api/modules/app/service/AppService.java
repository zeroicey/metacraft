package com.metacraft.api.modules.app.service;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        log.info("Created new version {} for app {}", nextVersion, appId);
        return version;
    }
    
    /**
     * 获取应用
     */
    public AppEntity getApp(Long appId) {
        return appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App not found: " + appId));
    }
}
