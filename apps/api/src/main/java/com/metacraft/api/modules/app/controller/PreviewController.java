package com.metacraft.api.modules.app.controller;

import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.storage.service.StorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final StorageService storageService;

    /**
     * 预览应用最新版本 (通过 UUID)
     * URL: /preview/{uuid}
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<String> previewLatest(@PathVariable String uuid, HttpServletRequest request) {
        AppEntity app = getAppByUuid(uuid);
        if (app.getCurrentVersionId() == null) {
            return ResponseEntity.ok("App has no versions yet.");
        }

        Long currentVersionId = Objects.requireNonNull(app.getCurrentVersionId());
        AppVersionEntity version = appVersionRepository.findById(currentVersionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        String htmlContent = storageService.readTextFile(version.getStoragePath());
        String htmlWithBase = injectBaseHref(htmlContent, buildBaseHref(request));

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
                .body(htmlWithBase);
    }

    /**
     * 预览指定版本 (通过 UUID + Version Number)
     * URL: /preview/{uuid}/v/{versionNumber}
     */
    @GetMapping("/{uuid}/v/{versionNumber}")
    public ResponseEntity<String> previewVersion(
            @PathVariable String uuid,
            @PathVariable Integer versionNumber,
            HttpServletRequest request
    ) {
        AppEntity app = getAppByUuid(uuid);
        AppVersionEntity version = appVersionRepository.findByAppIdAndVersionNumber(app.getId(), versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found: v" + versionNumber));
        String htmlContent = storageService.readTextFile(version.getStoragePath());
        String htmlWithBase = injectBaseHref(htmlContent, buildBaseHref(request));

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
                .body(htmlWithBase);
    }

    /**
     * 预览应用最新版本的 app.js
     * URL: /preview/{uuid}/app.js
     */
    @GetMapping(value = "/{uuid}/app.js", produces = "application/javascript")
    public ResponseEntity<String> previewLatestJs(@PathVariable String uuid) {
        AppEntity app = getAppByUuid(uuid);
        if (app.getCurrentVersionId() == null) {
            return ResponseEntity.ok("");
        }

        Long currentVersionId = Objects.requireNonNull(app.getCurrentVersionId());
        AppVersionEntity version = appVersionRepository.findById(currentVersionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        String jsContent = storageService.readTextFile(toJsPath(version.getStoragePath()));

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(jsContent);
    }

    /**
     * 预览指定版本的 app.js
     * URL: /preview/{uuid}/v/{versionNumber}/app.js
     */
    @GetMapping(value = "/{uuid}/v/{versionNumber}/app.js", produces = "application/javascript")
    public ResponseEntity<String> previewVersionJs(
            @PathVariable String uuid,
            @PathVariable Integer versionNumber
    ) {
        AppEntity app = getAppByUuid(uuid);
        AppVersionEntity version = appVersionRepository.findByAppIdAndVersionNumber(app.getId(), versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found: v" + versionNumber));
        String jsContent = storageService.readTextFile(toJsPath(version.getStoragePath()));

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(jsContent);
    }

    private AppEntity getAppByUuid(String uuid) {
        return appRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("App not found"));
    }

    private String toJsPath(String htmlPath) {
        return htmlPath.endsWith("index.html")
                ? htmlPath.substring(0, htmlPath.length() - "index.html".length()) + "app.js"
                : htmlPath + "/app.js";
    }

    private String injectBaseHref(String htmlContent, String baseHref) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return htmlContent;
        }
        String baseTag = "<base href='" + baseHref + "'>";
        if (htmlContent.contains("<head>")) {
            return htmlContent.replaceFirst("<head>", "<head>" + baseTag);
        }
        if (htmlContent.contains("<head ")) {
            return htmlContent.replaceFirst("<head[^>]*>", "$0" + baseTag);
        }
        return baseTag + htmlContent;
    }

    private String buildBaseHref(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
