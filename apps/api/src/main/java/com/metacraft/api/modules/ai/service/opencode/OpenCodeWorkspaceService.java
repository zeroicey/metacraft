package com.metacraft.api.modules.ai.service.opencode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.metacraft.api.modules.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenCodeWorkspaceService {

    private final StorageService storageService;

    public Path ensureWorkspaceRoot() {
        Path rootLocation = storageService.getRootLocation();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize OpenCode workspace root: " + rootLocation,
                    exception);
        }
        return rootLocation;
    }

    public Path getWorkspaceRoot() {
        return storageService.getRootLocation();
    }

    public Path getAppDirectory(Long appId) {
        return ensureWorkspaceRoot().resolve("apps").resolve(String.valueOf(appId)).normalize();
    }

    public Path getAppVersionDirectory(Long appId, Integer versionNumber) {
        return getAppDirectory(appId).resolve("v" + versionNumber).normalize();
    }
}