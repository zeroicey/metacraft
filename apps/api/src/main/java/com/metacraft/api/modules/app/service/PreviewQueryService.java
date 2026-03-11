package com.metacraft.api.modules.app.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreviewQueryService {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final StorageService storageService;

    public String getLatestStoragePath(String uuid) {
        return getCurrentVersion(uuid).getStoragePath();
    }

    public String getVersionStoragePath(String uuid, Integer versionNumber) {
        return getVersion(uuid, versionNumber).getStoragePath();
    }

    public String readRequiredTextFile(String relativePath) {
        if (!storageService.exists(relativePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return storageService.readTextFile(relativePath);
    }

    private AppVersionEntity getCurrentVersion(String uuid) {
        AppEntity app = getAppByUuid(uuid);
        Long currentVersionId = app.getCurrentVersionId();
        if (currentVersionId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Current version not found");
        }
        return appVersionRepository.findById(currentVersionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }

    private AppVersionEntity getVersion(String uuid, Integer versionNumber) {
        AppEntity app = getAppByUuid(uuid);
        return appVersionRepository.findByAppIdAndVersionNumber(app.getId(), versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found: v" + versionNumber));
    }

    private AppEntity getAppByUuid(String uuid) {
        return appRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not found"));
    }
}