package com.metacraft.api.modules.app.service;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class PreviewService {

    private static final Set<String> SUPPORTED_ASSET_EXTENSIONS = Set.of("js", "css");

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final StorageService storageService;

    public PreviewResource getLatestHtml(String uuid) {
        AppVersionEntity version = getCurrentVersion(uuid);
        return new PreviewResource(readRequiredTextFile(version.getStoragePath()), Objects.requireNonNull(MediaType.TEXT_HTML));
    }

    public PreviewResource getVersionHtml(String uuid, Integer versionNumber) {
        AppVersionEntity version = getVersion(uuid, versionNumber);
        return new PreviewResource(readRequiredTextFile(version.getStoragePath()), Objects.requireNonNull(MediaType.TEXT_HTML));
    }

    public PreviewResource getLatestAsset(String uuid, String fileName, String extension) {
        AppVersionEntity version = getCurrentVersion(uuid);
        return buildAssetResource(version.getStoragePath(), fileName, extension);
    }

    public PreviewResource getVersionAsset(String uuid, Integer versionNumber, String fileName, String extension) {
        AppVersionEntity version = getVersion(uuid, versionNumber);
        return buildAssetResource(version.getStoragePath(), fileName, extension);
    }

    private PreviewResource buildAssetResource(String htmlPath, String fileName, String extension) {
        String assetPath = toAssetPath(htmlPath, fileName, extension);
        return new PreviewResource(readRequiredTextFile(assetPath), resolveAssetMediaType(extension));
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

    private String readRequiredTextFile(String relativePath) {
        if (!storageService.exists(relativePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return storageService.readTextFile(relativePath);
    }

    private String toAssetPath(String htmlPath, String fileName, String extension) {
        String normalizedExtension = normalizeAssetExtension(extension);
        String assetFileName = fileName + "." + normalizedExtension;
        return htmlPath.endsWith("index.html")
                ? htmlPath.substring(0, htmlPath.length() - "index.html".length()) + assetFileName
                : htmlPath + "/" + assetFileName;
    }

    private MediaType resolveAssetMediaType(String extension) {
        String normalizedExtension = normalizeAssetExtension(extension);
        return switch (normalizedExtension) {
            case "js" -> MediaType.valueOf("application/javascript");
            case "css" -> Objects.requireNonNull(MediaType.parseMediaType("text/css"));
            default -> throw new IllegalArgumentException("Unsupported extension: " + extension);
        };
    }

    private String normalizeAssetExtension(String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ASSET_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("Unsupported extension: " + extension);
        }
        return normalizedExtension;
    }

    public record PreviewResource(String content, MediaType mediaType) {
    }
}