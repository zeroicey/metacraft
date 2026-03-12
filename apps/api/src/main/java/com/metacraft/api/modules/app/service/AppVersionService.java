package com.metacraft.api.modules.app.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final AppCodeAssetService appCodeAssetService;

    @Transactional
    public AppVersionEntity createVersion(Long appId, String htmlContent, String jsContent, String changeLog) {
        return appCodeAssetService.createVersion(appId, htmlContent, jsContent, changeLog);
    }

    @Transactional
    public AppVersionEntity createVersionFromSnapshot(Long appId, String htmlContent, String jsContent, String changeLog) {
        return appCodeAssetService.createVersionFromSnapshot(appId, htmlContent, jsContent, changeLog);
    }

    @Transactional
    public void deleteVersion(Long userId, Long appId, Long versionId) {
        AppEntity app = getOwnedApp(userId, appId);
        AppVersionEntity version = appVersionRepository.findById(Objects.requireNonNull(versionId))
                .orElseThrow(() -> new IllegalArgumentException("Version not found with id: " + versionId));

        if (!version.getAppId().equals(appId)) {
            throw new IllegalArgumentException("Version does not belong to this app");
        }

        appCodeAssetService.deleteVersionFiles(version);

        if (app.getCurrentVersionId() != null && app.getCurrentVersionId().equals(versionId)) {
            List<AppVersionEntity> remainingVersions = appVersionRepository.findByAppIdOrderByVersionNumberDesc(appId)
                    .stream()
                    .filter(candidate -> !candidate.getId().equals(versionId))
                    .toList();

            app.setCurrentVersionId(remainingVersions.isEmpty() ? null : remainingVersions.get(0).getId());
            appRepository.save(app);
        }

        appVersionRepository.delete(version);
        log.info("Deleted version {} of app {}", versionId, appId);
    }

    private AppEntity getOwnedApp(Long userId, Long appId) {
        AppEntity app = appRepository.findById(Objects.requireNonNull(appId))
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return app;
    }
}