package com.metacraft.api.modules.app.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppLogoService {

    private static final String[] EXTS = new String[]{"png", "jpg", "jpeg", "webp"};

    private final AppRepository appRepository;
    private final StorageService storageService;

    public Optional<LogoResource> findLogoByUuid(String uuid) {
        for (String ext : EXTS) {
            String relativePath = "logos/" + uuid + "." + ext;
            if (!storageService.exists(relativePath)) {
                continue;
            }

            return Optional.of(new LogoResource(
                    storageService.readBinaryFile(relativePath),
                    toMediaType(ext)));
        }

        return Optional.empty();
    }

    public Optional<LogoResource> findLogoByAppId(Long appId) {
        return appRepository.findById(Objects.requireNonNull(appId))
                .map(AppEntity::getLogo)
                .filter(logo -> !logo.isBlank())
                .filter(logo -> storageService.exists("logos/" + logo))
                .map(logo -> new LogoResource(
                        storageService.readBinaryFile("logos/" + logo),
                        toMediaType(resolveExt(logo))));
    }

    public String generateLogoAndSave(AppEntity app, String logoUuid) {
        // TODO: 恢复 Logo 生成实现。
        throw new UnsupportedOperationException("Logo generation pipeline is disabled");
    }

    private String resolveExt(String logo) {
        int dotIndex = logo.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == logo.length() - 1) {
            return "png";
        }
        return logo.substring(dotIndex + 1).toLowerCase();
    }

    private MediaType toMediaType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_PNG;
        };
    }

    public record LogoResource(byte[] content, MediaType mediaType) {
    }
}