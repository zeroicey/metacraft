package com.metacraft.api.modules.app.service;

import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreviewService {

    private final PreviewQueryService previewQueryService;
    private final PreviewAssetResolver previewAssetResolver;

    public PreviewResource getLatestHtml(String uuid) {
        String storagePath = previewQueryService.getLatestStoragePath(uuid);
        return new PreviewResource(
                previewQueryService.readRequiredTextFile(storagePath),
                Objects.requireNonNull(MediaType.TEXT_HTML));
    }

    public PreviewResource getVersionHtml(String uuid, Integer versionNumber) {
        String storagePath = previewQueryService.getVersionStoragePath(uuid, versionNumber);
        return new PreviewResource(
                previewQueryService.readRequiredTextFile(storagePath),
                Objects.requireNonNull(MediaType.TEXT_HTML));
    }

    public PreviewResource getLatestAsset(String uuid, String fileName, String extension) {
        return buildAssetResource(previewQueryService.getLatestStoragePath(uuid), fileName, extension);
    }

    public PreviewResource getVersionAsset(String uuid, Integer versionNumber, String fileName, String extension) {
        return buildAssetResource(previewQueryService.getVersionStoragePath(uuid, versionNumber), fileName, extension);
    }

    private PreviewResource buildAssetResource(String htmlPath, String fileName, String extension) {
        String assetPath = previewAssetResolver.toAssetPath(htmlPath, fileName, extension);
        return new PreviewResource(
                previewQueryService.readRequiredTextFile(assetPath),
                previewAssetResolver.resolveAssetMediaType(extension));
    }

    public record PreviewResource(String content, MediaType mediaType) {
    }
}