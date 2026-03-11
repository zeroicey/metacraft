package com.metacraft.api.modules.app.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.dto.AppCodeSnapshotDTO;
import com.metacraft.api.modules.app.assembler.AppAssembler;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.app.vo.AppVO;
import com.metacraft.api.modules.app.vo.AppVersionVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppQueryService {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final AppCodeAssetService appCodeAssetService;
    private final AppAssembler appAssembler;

    public List<AppVO> getUserApps(Long userId) {
        return appRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toAppVO)
                .toList();
    }

    public AppVO getAppByUuid(Long userId, String uuid) {
        return toAppVO(getOwnedAppByUuid(userId, uuid));
    }

    public AppVO getAppById(Long userId, Long appId) {
        return toAppVO(getOwnedAppById(userId, appId));
    }

    public AppCodeSnapshotDTO getCurrentCode(Long userId, Long appId) {
        return appCodeAssetService.getCurrentCode(getOwnedAppById(userId, appId));
    }

    public List<AppVersionVO> getAppVersions(Long userId, Long appId) {
        getOwnedAppById(userId, appId);
        return appAssembler.toVersionVOs(appVersionRepository.findByAppIdOrderByVersionNumberDesc(appId));
    }

    public AppVersionVO getVersion(Long userId, Long appId, Integer versionNumber) {
        getOwnedAppById(userId, appId);
        AppVersionEntity version = appVersionRepository.findByAppIdAndVersionNumber(appId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionNumber));
        return appAssembler.toVersionVO(version);
    }

    private AppVO toAppVO(AppEntity app) {
        return appAssembler.toAppVO(app, appVersionRepository.findByAppIdOrderByVersionNumberDesc(app.getId()));
    }

    private AppEntity getOwnedAppById(Long userId, Long appId) {
        AppEntity app = appRepository.findById(Objects.requireNonNull(appId))
                .orElseThrow(() -> new IllegalArgumentException("App not found with id: " + appId));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return app;
    }

    private AppEntity getOwnedAppByUuid(Long userId, String uuid) {
        AppEntity app = appRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("App not found with uuid: " + uuid));

        if (!app.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this app");
        }

        return app;
    }
}