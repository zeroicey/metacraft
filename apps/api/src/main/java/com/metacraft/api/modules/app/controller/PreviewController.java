package com.metacraft.api.modules.app.controller;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.repository.AppVersionRepository;
import com.metacraft.api.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> previewLatest(@PathVariable String uuid) {
        // 1. 通过 UUID 查找应用 (防止遍历攻击)
        AppEntity app = appRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("App not found"));

        // 2. 获取当前版本ID
        if (app.getCurrentVersionId() == null) {
            return ResponseEntity.ok("App has no versions yet.");
        }

        // 3. 获取版本详情
        AppVersionEntity version = appVersionRepository.findById(app.getCurrentVersionId())
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // 4. 读取文件内容
        String htmlContent = storageService.readTextFile(version.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlContent);
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
        // 1. 通过 UUID 查找应用
        AppEntity app = appRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("App not found"));

        // 2. 查找指定版本
        AppVersionEntity version = appVersionRepository.findByAppIdAndVersionNumber(app.getId(), versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found: v" + versionNumber));

        // 3. 读取文件内容
        String htmlContent = storageService.readTextFile(version.getStoragePath());

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlContent);
    }
}
