package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.service.AppService;
import com.metacraft.api.modules.ai.util.SseUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolService {

    private final AppService appService;
    private final ImageService imageService;
    private final GenerationTaskService generationTaskService;
    private final GenerationArtifactService generationArtifactService;
    private final RunSseEventService runSseEventService;
    private final SseUtils sseUtils;

    @Tool("Save the generated application code, name, and description. Returns the preview URL.")
    public String saveApp(
            @P("The run ID provided in the context") String runId,
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
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId is required to save app");
        }

        generationTaskService.markRunning(runId, "save");
        
        log.info("Saving app for run {} user {}: name={}, description={}", runId, userId, name, description);

        try {
            AppEntity app = appService.createApp(userId, name, description);
            appService.updateAppLogo(app.getId(), logoUuid + ".png");
            appService.createVersion(app.getId(), code, "Initial generation by AI");

            AppEntity persisted = appService.getApp(app.getId());
            String previewUrl = "/api/preview/" + persisted.getUuid();

            generationArtifactService.saveArtifact(
                    runId,
                    "app_saved",
                    persisted.getId(),
                    Map.of(
                            "appId", persisted.getId(),
                            "appUuid", persisted.getUuid(),
                            "versionId", persisted.getCurrentVersionId(),
                            "name", persisted.getName(),
                            "description", persisted.getDescription(),
                            "previewUrl", previewUrl,
                            "logoUrl", "/api/logo/" + logoUuid
                    )
            );
            generationTaskService.markSuccess(runId, "save");
            return "应用已保存，预览链接：" + previewUrl;
        } catch (Exception e) {
            generationTaskService.markFailed(runId, "save", e.getMessage());
            throw e;
        }
    }

    @Tool("Generate app logo asynchronously and store it by logo UUID. This tool is non-blocking.")
    public String generateAppLogoAsync(
            @P("The run ID provided in the context") String runId,
            @P("The app name") String name,
            @P("The app description") String description,
            @P("The pre-generated logo UUID from context") String logoUuid
    ) {
        if (runId == null || runId.isBlank()) {
            return "Logo task skipped: missing run ID.";
        }
        if (logoUuid == null || logoUuid.isBlank()) {
            return "Logo task skipped: missing logo UUID.";
        }

        boolean started = generationTaskService.tryMarkRunning(runId, "logo");
        if (!started) {
            return "Logo task already started: /api/logo/" + logoUuid;
        }

        runSseEventService.publish(runId, "logo_started", sseUtils.toLogoStartedJson(runId, logoUuid));

        CompletableFuture<String> logoFuture = imageService.generateAndSaveLogoAsync(name, description, logoUuid);
        logoFuture.whenComplete((logoUrl, throwable) -> {
            if (throwable == null) {
                generationTaskService.markSuccess(runId, "logo");
                generationArtifactService.saveArtifact(
                        runId,
                        "logo_ready",
                        null,
                        Map.of("logoUrl", logoUrl, "logoUuid", logoUuid)
                );
                runSseEventService.publish(runId, "logo_ready", sseUtils.toLogoReadyJson(runId, logoUrl));
                return;
            }

            String reason = throwable.getMessage() != null ? throwable.getMessage() : "logo generation failed";
            generationTaskService.markFailed(runId, "logo", reason);
            generationArtifactService.saveArtifact(
                    runId,
                    "logo_failed",
                    null,
                    Map.of("reason", reason, "logoUuid", logoUuid)
            );
            runSseEventService.publish(runId, "logo_failed", sseUtils.toLogoFailedJson(runId, reason));
        });

        return "Logo task started: /api/logo/" + logoUuid;
    }
}
