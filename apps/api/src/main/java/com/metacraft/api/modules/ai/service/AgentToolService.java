package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.service.AppService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService {

    private final AppService appService;
    private final ImageService imageService;

    @Tool("Save the generated application code, name, and description. Returns the preview URL.")
    public String saveApp(
            @P("The name of the application") String name,
            @P("A short description of the application") String description,
            @P("The complete HTML source code of the application") String code,
        @P("The pre-generated logo UUID from context") String logoUuid,
            @P("The user ID provided in the context") Long userId
    ) {
        // 1. 获取当前用户
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to save app");
        }
        if (logoUuid == null || logoUuid.isBlank()) {
            throw new IllegalArgumentException("logoUuid is required to save app");
        }
        
        log.info("Saving app for user {}: name={}, description={}", userId, name, description);

        // 2. 调用 AppService 保存应用
        AppEntity app = appService.createApp(userId, name, description);
        // Keep db value as uuid.ext. Async logo task may later overwrite extension if needed.
        appService.updateAppLogo(app.getId(), logoUuid + ".png");
        appService.createVersion(app.getId(), code, "Initial generation by AI");

        // 3. 返回预览链接
        String previewUrl = "/api/preview/" + app.getUuid() + "/v/1";
        return "应用已保存，预览链接：" + previewUrl;
    }

    @Tool("Generate app logo asynchronously and store it by logo UUID. This tool is non-blocking.")
    public String generateAppLogoAsync(
            @P("The app name") String name,
            @P("The app description") String description,
            @P("The pre-generated logo UUID from context") String logoUuid
    ) {
        if (logoUuid == null || logoUuid.isBlank()) {
            return "Logo task skipped: missing logo UUID.";
        }

        imageService.generateAndSaveLogoAsync(name, description, logoUuid);
        return "Logo task started: /api/logo/" + logoUuid;
    }
}
