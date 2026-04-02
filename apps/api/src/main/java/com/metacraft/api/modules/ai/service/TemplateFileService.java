package com.metacraft.api.modules.ai.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TemplateFileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".html", ".js", ".css", ".json", ".vue", ".png", ".jpg", ".jpeg", ".svg", ".gif", ".woff2"
    );

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".sh", ".bat", ".cmd", ".exe", ".ps1"
    );

    @Value("${app.templates.path:data/templates}")
    private String templatesPath;

    @Value("${app.storage.path:data}")
    private String storagePath;

    /**
     * Copy all files from template directory to destination directory.
     *
     * @param templateName   The matched template folder name
     * @param appId         The app ID
     * @param versionNumber The version number
     * @return true if successful, false if should fallback to CodeGenerator
     */
    public boolean copyTemplateFiles(String templateName, Long appId, Integer versionNumber) {
        Path sourceDir = Path.of(templatesPath, templateName);
        Path destDir = Path.of(storagePath, "apps", appId.toString(), "v" + versionNumber);

        // Validate source path is within templates directory (prevent traversal)
        try {
            sourceDir = sourceDir.toRealPath();
            Path templatesRealPath = Path.of(templatesPath).toRealPath();
            if (!sourceDir.startsWith(templatesRealPath)) {
                log.error("Template path traversal attempt detected: {}", sourceDir);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to resolve template path: {}", sourceDir, e);
            return false;
        }

        if (!Files.exists(sourceDir)) {
            log.warn("Template directory not found: {}", sourceDir);
            return false;
        }

        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            log.error("Failed to create destination directory: {}", destDir, e);
            return false;
        }

        // Validate destination path is within storage directory
        try {
            Path destRealPath = destDir.toRealPath();
            Path storageRealPath = Path.of(storagePath).toRealPath();
            if (!destRealPath.startsWith(storageRealPath)) {
                log.error("Destination path traversal attempt detected: {}", destRealPath);
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to resolve destination path: {}", destDir, e);
            return false;
        }

        File[] files = sourceDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            log.warn("Template directory is empty: {}", sourceDir);
            return false;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String fileName = file.getName();
            String extension = getFileExtension(fileName).toLowerCase();

            // Check forbidden extensions
            if (FORBIDDEN_EXTENSIONS.contains(extension)) {
                log.warn("Forbidden file type in template: {}", fileName);
                return false;
            }

            // Skip files with no extension unless it's allowed
            if (!extension.isEmpty() && !ALLOWED_EXTENSIONS.contains(extension)) {
                log.warn("File extension not allowed: {}", fileName);
                return false;
            }

            try {
                Path sourceFile = file.toPath();
                Path destFile = destDir.resolve(fileName);
                Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied template file: {} -> {}", fileName, destFile);
            } catch (IOException e) {
                log.error("Failed to copy template file: {}", fileName, e);
                return false;
            }
        }

        log.info("Successfully copied {} template files to {}", files.length, destDir);
        return true;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }
}