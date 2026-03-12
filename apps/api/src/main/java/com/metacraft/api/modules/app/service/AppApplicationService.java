package com.metacraft.api.modules.app.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metacraft.api.modules.ai.service.opencode.OpenCodeClient;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeClientException;
import com.metacraft.api.modules.app.dto.AppCreateDTO;
import com.metacraft.api.modules.app.dto.AppUpdateDTO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings("null")
@RequiredArgsConstructor
public class AppApplicationService {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final AppCodeAssetService appCodeAssetService;
    private final OpenCodeClient openCodeClient;

    @Transactional
    public AppEntity createApp(Long userId, String name, String description) {
        AppEntity app = AppEntity.builder()
                .userId(userId)
                .name(name)
                .uuid(java.util.UUID.randomUUID().toString())
                .description(description)
                .isPublic(false)
                .build();
            return Objects.requireNonNull(appRepository.save(app));
    }

    @Transactional
    public AppEntity createApp(Long userId, AppCreateDTO dto) {
        AppEntity saved = createApp(userId, dto.getName(), dto.getDescription());
        log.info("Created new app with id={} and uuid={}", saved.getId(), saved.getUuid());
        return saved;
    }

    public AppEntity getApp(Long appId) {
        return appRepository.findById(Objects.requireNonNull(appId))
                .orElseThrow(() -> new RuntimeException("App not found: " + appId));
    }

    @Transactional
    public void updateAppMetadata(Long appId, String name, String description) {
        appRepository.findById(Objects.requireNonNull(appId)).ifPresent(app -> {
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

    @Transactional
    public void updateAppLogo(Long appId, String logo) {
        appRepository.findById(Objects.requireNonNull(appId)).ifPresent(app -> {
            app.setLogo(logo);
            appRepository.save(app);
            log.info("Updated logo for app {}: logo={}", appId, logo);
        });
    }

    @Transactional
    public void bindOpenCodeSessionId(Long appId, String openCodeSessionId) {
        appRepository.findById(Objects.requireNonNull(appId)).ifPresent(app -> {
            app.setOpenCodeSessionId(openCodeSessionId);
            appRepository.save(app);
            log.info("Bound OpenCode session {} to app {}", openCodeSessionId, appId);
        });
    }

    public AppEntity getLatestAppByUserId(Long userId) {
        return appRepository.findTopByUserIdOrderByIdDesc(userId)
                .orElse(null);
    }

    public AppEntity getLatestAppByUserIdAndLogoUuid(Long userId, String logoUuid) {
        if (logoUuid == null || logoUuid.isBlank()) {
            return null;
        }
        return appRepository.findTopByUserIdAndLogoStartingWithOrderByIdDesc(userId, logoUuid)
                .orElse(null);
    }

    @Transactional
    public AppEntity updateApp(Long userId, Long appId, AppUpdateDTO dto) {
        AppEntity app = getOwnedApp(userId, appId);

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            app.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            app.setDescription(dto.getDescription());
        }
        if (dto.getIsPublic() != null) {
            app.setIsPublic(dto.getIsPublic());
        }

        AppEntity updated = Objects.requireNonNull(appRepository.save(app));
        log.info("Updated app {} metadata", appId);
        return updated;
    }

    @Transactional
    public void deleteApp(Long userId, Long appId) {
        AppEntity app = getOwnedApp(userId, appId);
        deleteOpenCodeSession(app);
        List<AppVersionEntity> versions = appVersionRepository.findByAppIdOrderByVersionNumberDesc(appId);

        for (AppVersionEntity version : versions) {
            appCodeAssetService.deleteVersionFiles(version);
        }

        appVersionRepository.deleteAll(List.copyOf(versions));
        appRepository.delete(app);

        log.info("Deleted app {} and all its versions", appId);
    }

    private void deleteOpenCodeSession(AppEntity app) {
        String openCodeSessionId = app.getOpenCodeSessionId();
        if (openCodeSessionId == null || openCodeSessionId.isBlank()) {
            return;
        }

        try {
            openCodeClient.deleteSession(openCodeSessionId);
            log.info("Deleted OpenCode session {} for app {}", openCodeSessionId, app.getId());
        } catch (OpenCodeClientException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("-> 404")) {
                log.info("OpenCode session {} already missing for app {}", openCodeSessionId, app.getId());
                return;
            }
            throw new IllegalStateException(
                    "Failed to delete OpenCode session " + openCodeSessionId + " for app " + app.getId(),
                    exception);
        }
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