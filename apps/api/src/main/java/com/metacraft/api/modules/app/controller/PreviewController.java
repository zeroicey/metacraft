package com.metacraft.api.modules.app.controller;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metacraft.api.modules.app.service.PreviewService;
import com.metacraft.api.modules.app.service.PreviewService.PreviewResource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;

    /**
     * 预览应用最新版本 (通过 UUID)
     * URL: /preview/{uuid}
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<String> previewLatest(@PathVariable String uuid) {
        PreviewResource resource = previewService.getLatestHtml(uuid);
        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(resource.mediaType()))
                .body(resource.content());
    }

    /**
     * 预览指定版本 (通过 UUID + Version Number)
     * URL: /preview/{uuid}/v/{versionNumber}
     */
    @GetMapping("/{uuid}/v/{versionNumber}")
    public ResponseEntity<String> previewVersion(
            @PathVariable String uuid,
            @PathVariable Integer versionNumber
    ) {
        PreviewResource resource = previewService.getVersionHtml(uuid, versionNumber);
        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(resource.mediaType()))
                .body(resource.content());
    }

    /**
     * 预览应用最新版本的静态资源
     * URL: /preview/{uuid}/{fileName}.{extension}
     */
    @GetMapping(value = "/{uuid}/{fileName}.{extension}")
    public ResponseEntity<String> previewLatestAsset(
        @PathVariable String uuid,
        @PathVariable String fileName,
        @PathVariable String extension
    ) {
        PreviewResource resource = previewService.getLatestAsset(uuid, fileName, extension);

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(resource.mediaType()))
            .body(resource.content());
    }

    /**
     * 预览指定版本的静态资源
     * URL: /preview/{uuid}/v/{versionNumber}/{fileName}.{extension}
     */
    @GetMapping(value = "/{uuid}/v/{versionNumber}/{fileName}.{extension}")
    public ResponseEntity<String> previewVersionAsset(
        @PathVariable String uuid,
        @PathVariable Integer versionNumber,
        @PathVariable String fileName,
        @PathVariable String extension
    ) {
        PreviewResource resource = previewService.getVersionAsset(uuid, versionNumber, fileName, extension);

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(resource.mediaType()))
            .body(resource.content());
    }
}