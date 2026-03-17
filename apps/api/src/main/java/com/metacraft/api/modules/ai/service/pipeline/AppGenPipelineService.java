package com.metacraft.api.modules.ai.service.pipeline;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.AppInfoExtractor;
import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.agent.PlanGenerator;
import com.metacraft.api.modules.ai.dto.AppInfoDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.dto.TemplateMatchResult;
import com.metacraft.api.modules.ai.dto.opencode.OpenCodeDtos;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
import com.metacraft.api.modules.ai.service.TemplateMatcher;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeClient;
import com.metacraft.api.modules.ai.service.opencode.OpenCodePromptService;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeTemplateService;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeWorkspaceService;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.service.AppService;
import com.metacraft.api.modules.app.service.ImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppGenPipelineService {

    private final ChatAgent chatAgent;
    private final PlanGenerator planGenerator;
    private final AppInfoExtractor appInfoExtractor;
    private final AppService appService;
    private final ImageService imageService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final TemplateMatcher templateMatcher;
    private final OpenCodeClient openCodeClient;
    private final OpenCodePromptService openCodePromptService;
    private final OpenCodeTemplateService openCodeTemplateService;
    private final OpenCodeWorkspaceService openCodeWorkspaceService;
    private final SseUtils sseUtils;

    public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
        StringBuffer chatBeforeGenBuffer = new StringBuffer();
        StringBuffer planBuffer = new StringBuffer();
        AtomicReference<Long> relatedAppIdRef = new AtomicReference<>();
        AtomicReference<Long> relatedVersionIdRef = new AtomicReference<>();

        Flux<ServerSentEvent<String>> chatStream = chatAgent.chatBeforeGen(message, history)
                .doOnNext(chatBeforeGenBuffer::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(sseUtils.toContentJson(chunk))
                        .build());

        Flux<ServerSentEvent<String>> planStream = planGenerator.generatePlan(message)
                .doOnNext(planBuffer::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("plan")
                        .data(sseUtils.toPlanJson(chunk))
                        .build());

        Mono<AppEntity> appInfoMono = Mono.fromCallable(() -> createAppFromExtractedInfo(userId, sessionId, message))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(app -> relatedAppIdRef.set(app.getId()))
                .cache();

        Flux<ServerSentEvent<String>> appInfoStream = appInfoMono.map(app -> ServerSentEvent.<String>builder()
                .event("app_info")
                .data(sseUtils.toAppInfoJson(app.getName(), app.getDescription()))
                .build()).flux();

        Flux<ServerSentEvent<String>> postAppInfoStream = appInfoMono.flatMapMany(app -> {
            Flux<ServerSentEvent<String>> logoStream = Mono.fromCallable(() -> generateLogoEvent(app))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flux();

            // 模板匹配 - 立即启动，与其他流程并行
            Mono<TemplateMatchResult> templateMatchMono = Mono.fromCallable(() ->
                    templateMatcher.matchWithTimeout(message, Duration.ofSeconds(3))
                ).subscribeOn(Schedulers.boundedElastic())
                .cache();

            // 检查模板匹配结果
            Flux<ServerSentEvent<String>> codeStream = templateMatchMono.flatMapMany(matchResult -> {
                if (matchResult.isMatched()) {
                    // 使用模板代码
                    return Mono.fromCallable(() -> {
                        AppVersionEntity version = appService.createVersion(
                            app.getId(),
                            matchResult.getTemplate().getHtmlContent(),
                            matchResult.getTemplate().getJsContent(),
                            "使用模板: " + matchResult.getTemplateName()
                        );
                        relatedVersionIdRef.set(version.getId());
                        log.info("Using template {} for app {}", matchResult.getTemplateName(), app.getId());
                        return ServerSentEvent.<String>builder()
                            .event("app_generated")
                            .data(sseUtils.toAppGeneratedJson(app.getUuid(), version.getVersionNumber()))
                            .build();
                    }).subscribeOn(Schedulers.boundedElastic()).flux();
                } else {
                    // 降级到 OpenCode 生成
                    return Mono.fromCallable(() -> {
                        AppVersionEntity createdVersion = generateInitialVersionWithOpenCode(app, history, message);
                        relatedVersionIdRef.set(createdVersion.getId());
                        return ServerSentEvent.<String>builder()
                            .event("app_generated")
                            .data(sseUtils.toAppGeneratedJson(app.getUuid(), createdVersion.getVersionNumber()))
                            .build();
                    }).subscribeOn(Schedulers.boundedElastic()).flux();
                }
            });

            return Flux.merge(logoStream, codeStream);
        });

        return Flux.merge(chatStream, planStream, appInfoStream, postAppInfoStream)
                .doOnComplete(() -> saveAssistantGenMessage(
                        userId,
                        sessionId,
                        chatBeforeGenBuffer.toString(),
                        planBuffer.toString(),
                        relatedAppIdRef.get(),
                        relatedVersionIdRef.get()));
    }

    private String buildOpenCodePrompt(String targetDir, String history, String message) {
        return openCodePromptService.buildAppGenPrompt(targetDir)
                + "\n\nConversation history:\n" + safeHistory(history)
                + "\n\nCurrent user request:\n" + message;
    }

    private AppEntity createAppFromExtractedInfo(Long userId, String sessionId, String message) {
        AppInfoDTO info = appInfoExtractor.extract(message);
        String appName = normalizeAppName(info != null ? info.getName() : null, message);
        String appDescription = normalizeAppDescription(info != null ? info.getDescription() : null, message);

        AppEntity app = appService.createApp(userId, appName, appDescription);
        chatSessionService.bindRelatedApp(userId, sessionId, app.getId());
        return app;
    }

    private ServerSentEvent<String> generateLogoEvent(AppEntity app) {
        String logoUuid = UUID.randomUUID().toString();
        String ext = imageService.generateLogoAndSave(app, logoUuid);
        appService.updateAppLogo(app.getId(), logoUuid + "." + ext);
        return ServerSentEvent.<String>builder()
                .event("logo_generated")
                .data(sseUtils.toLogoGerneratedJson(logoUuid, ext))
                .build();
    }

    private AppVersionEntity generateInitialVersionWithOpenCode(AppEntity app, String history, String message) {
        AppVersionEntity version = appService.createVersion(
                app.getId(),
                openCodeTemplateService.loadIndexHtmlTemplate(),
                openCodeTemplateService.loadAppJsTemplate(),
                "Initialize template for new app");

        String targetDir = openCodeWorkspaceService.getWorkspaceRelativeAppVersionDirectory(app.getId(),
                version.getVersionNumber());
        OpenCodeDtos.Session openCodeSession = openCodeClient.createSession(app.getUuid());
        appService.bindOpenCodeSessionId(app.getId(), openCodeSession.id());

        openCodeClient.sendMessage(openCodeSession.id(), OpenCodeDtos.MessageRequest.text(
                buildOpenCodePrompt(targetDir, history, message)));

        log.info("Generated app {} version {} in OpenCode session {}", app.getId(), version.getVersionNumber(),
                openCodeSession.id());
        return version;
    }

    private String safeHistory(String history) {
        return history == null || history.isBlank() ? "无历史对话。" : history;
    }

    private String normalizeAppName(String extractedName, String message) {
        if (extractedName != null && !extractedName.isBlank()) {
            return extractedName.length() > 60 ? extractedName.substring(0, 60) : extractedName;
        }

        String normalized = message == null ? "" : message.trim();
        if (normalized.isBlank()) {
            return "未命名应用";
        }
        return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
    }

    private String normalizeAppDescription(String extractedDescription, String message) {
        if (extractedDescription != null && !extractedDescription.isBlank()) {
            return extractedDescription.length() > 200 ? extractedDescription.substring(0, 200)
                    : extractedDescription;
        }
        if (message == null || message.isBlank()) {
            return "由 MetaCraft 自动创建的应用";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }

    private void saveAssistantGenMessage(
            Long userId,
            String sessionId,
            String chatBeforeGenContent,
            String planContent,
            Long relatedAppId,
            Long relatedVersionId) {
        String mergedContent = mergeGenAssistantContent(chatBeforeGenContent, planContent);
        if (mergedContent.isBlank()) {
            log.warn("Skip saving empty GEN assistant message for session {}", sessionId);
            return;
        }

        ChatMessageCreateDTO assistantMessageDto = new ChatMessageCreateDTO();
        assistantMessageDto.setSessionId(sessionId);
        assistantMessageDto.setRole("assistant");
        assistantMessageDto.setType("app");
        assistantMessageDto.setContent(mergedContent);
        assistantMessageDto.setRelatedAppId(relatedAppId);
        assistantMessageDto.setRelatedVersionId(relatedVersionId);
        chatMessageService.saveMessage(userId, assistantMessageDto);
        log.info("Saved GEN assistant message for session {}, appId={}, versionId={}",
                sessionId,
                relatedAppId,
                relatedVersionId);
    }

    private String mergeGenAssistantContent(String chatBeforeGenContent, String planContent) {
        String first = chatBeforeGenContent == null ? "" : chatBeforeGenContent.trim();
        String second = planContent == null ? "" : planContent.trim();
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + "\n\n" + second;
    }
}
