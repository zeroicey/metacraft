package com.metacraft.api.modules.app.service;

import java.util.List;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.metacraft.api.modules.ai.dto.AppCodeSnapshotDTO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.storage.service.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings("null")
@RequiredArgsConstructor
public class AppCodeAssetService {

    private final AppRepository appRepository;
    private final AppVersionRepository appVersionRepository;
    private final StorageService storageService;

    @Transactional
    public AppVersionEntity createVersion(Long appId, String htmlContent, String jsContent, String changeLog) {
        AppEntity app = appRepository.findById(Objects.requireNonNull(appId))
                .orElseThrow(() -> new RuntimeException("App not found: " + appId));

        Integer nextVersion = appVersionRepository.findTopByAppIdOrderByVersionNumberDesc(appId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        String formattedHtmlContent = prettyPrintHtml(htmlContent);
        String formattedJsContent = prettyPrintJavaScript(jsContent);
        String relativePath = String.format("apps/%d/v%d/index.html", appId, nextVersion);
        String jsRelativePath = toJsPath(relativePath);

        storageService.saveTextFile(relativePath, formattedHtmlContent);
        storageService.saveTextFile(jsRelativePath, formattedJsContent);

        AppVersionEntity version = AppVersionEntity.builder()
                .appId(appId)
                .versionNumber(nextVersion)
                .storagePath(relativePath)
                .changeLog(changeLog)
                .build();
        AppVersionEntity savedVersion = appVersionRepository.save(version);

        app.setCurrentVersionId(savedVersion.getId());
        appRepository.save(app);

        log.info("Created version {} (id={}) for app {}", nextVersion, savedVersion.getId(), appId);
        return savedVersion;
    }

    public AppCodeSnapshotDTO getCurrentCode(AppEntity app) {
        Long currentVersionId = app.getCurrentVersionId();
        if (currentVersionId == null) {
            throw new IllegalArgumentException("App has no versions yet: " + app.getId());
        }

        AppVersionEntity currentVersion = appVersionRepository.findById(currentVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Current version not found for app: " + app.getId()));

        String htmlContent = storageService.readTextFile(currentVersion.getStoragePath());
        String jsContent = storageService.readTextFile(toJsPath(currentVersion.getStoragePath()));
        return new AppCodeSnapshotDTO(
                htmlContent,
                jsContent,
                currentVersion.getId(),
                currentVersion.getVersionNumber());
    }

    public void deleteVersionFiles(AppVersionEntity version) {
        try {
            storageService.deleteFile(version.getStoragePath());
            storageService.deleteFile(toJsPath(version.getStoragePath()));
        } catch (Exception exception) {
            log.warn("Failed to delete file for version {}: {}", version.getId(), exception.getMessage());
        }
    }

    public String toJsPath(String htmlPath) {
        if (htmlPath == null || htmlPath.isBlank()) {
            return htmlPath;
        }
        return htmlPath.endsWith("index.html")
                ? htmlPath.substring(0, htmlPath.length() - "index.html".length()) + "app.js"
                : htmlPath + "/app.js";
    }

    private String prettyPrintHtml(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return htmlContent;
        }

        try {
            Document document = Jsoup.parse(htmlContent);
            document.outputSettings()
                    .prettyPrint(true)
                    .indentAmount(2);
            return document.outerHtml();
        } catch (Exception exception) {
            log.warn("Failed to pretty print HTML before saving, using original content", exception);
            return htmlContent;
        }
    }

    private String prettyPrintJavaScript(String jsContent) {
        if (jsContent == null || jsContent.isBlank()) {
            return jsContent;
        }

        try {
            Compiler compiler = new Compiler();
            CompilerOptions options = new CompilerOptions();
            CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(options);
            options.setPrettyPrint(true);
            options.setLineBreak(true);
            options.setPreferSingleQuotes(true);
            options.setPreserveTypeAnnotations(true);

            Result result = compiler.compile(
                    List.of(),
                    List.of(SourceFile.fromCode("app.js", jsContent)),
                    options);

            if (!result.success) {
                log.warn("Failed to pretty print JavaScript before saving, using original content: {}",
                        !result.errors.isEmpty() ? result.errors.get(0).getDescription() : "unknown error");
                return jsContent;
            }

            return compiler.toSource();
        } catch (Exception exception) {
            log.warn("Failed to pretty print JavaScript before saving, using original content", exception);
            return jsContent;
        }
    }
}