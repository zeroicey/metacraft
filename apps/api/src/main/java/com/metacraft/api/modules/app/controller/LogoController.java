package com.metacraft.api.modules.app.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.service.AppService;
import com.metacraft.api.modules.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/logo")
@RequiredArgsConstructor
public class LogoController {

    private static final String[] EXTS = new String[]{"png", "jpg", "jpeg", "webp"};

    private final AppService appService;
    private final StorageService storageService;

    @GetMapping("/{uuid}")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> getLogo(@PathVariable String uuid) {
        return findLogoByUuid(uuid);
    }

    @GetMapping("/app/{appId}")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> getLogoByAppId(@PathVariable Long appId) {
        try {
            AppEntity app = appService.getApp(appId);
            String logo = app.getLogo();
            if (logo == null || logo.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String relativePath = "logos/" + logo;
            if (!storageService.exists(relativePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] content = storageService.readBinaryFile(relativePath);
            return ResponseEntity.ok()
                    .contentType(toMediaType(resolveExt(logo)))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(content);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @SuppressWarnings("null")
    private ResponseEntity<byte[]> findLogoByUuid(String uuid) {
        for (String ext : EXTS) {
            String relativePath = "logos/" + uuid + "." + ext;
            if (!storageService.exists(relativePath)) {
                continue;
            }

            byte[] content = storageService.readBinaryFile(relativePath);
            return ResponseEntity.ok()
                    .contentType(toMediaType(ext))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(content);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
}
