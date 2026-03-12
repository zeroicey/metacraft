package com.metacraft.api.modules.ai.service.pipeline;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.dto.AppCodeSnapshotDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.dto.opencode.OpenCodeDtos;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeClient;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeClientException;
import com.metacraft.api.modules.ai.service.opencode.OpenCodePromptService;
import com.metacraft.api.modules.ai.service.opencode.OpenCodeWorkspaceService;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.ai.vo.ChatSessionVO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.service.AppService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppEditPipelineService {

    private final ChatAgent chatAgent;
    private final AppService appService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final OpenCodeClient openCodeClient;
    private final OpenCodePromptService openCodePromptService;
    private final OpenCodeWorkspaceService openCodeWorkspaceService;
    private final SseUtils sseUtils;

    public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
        StringBuffer chatBeforeEditBuffer = new StringBuffer();
        AtomicReference<Long> relatedAppIdRef = new AtomicReference<>();
        AtomicReference<Long> relatedVersionIdRef = new AtomicReference<>();

        Flux<ServerSentEvent<String>> chatStream = chatAgent.chatBeforeEdit(message, history)
            .doOnNext(chatBeforeEditBuffer::append)
            .map(chunk -> ServerSentEvent.<String>builder()
                .event("message")
                .data(sseUtils.toContentJson(chunk))
                .build());

        Flux<ServerSentEvent<String>> editStream = Mono.fromCallable(() -> {
            AppEntity app = resolveRelatedApp(userId, sessionId);
            relatedAppIdRef.set(app.getId());

            AppCodeSnapshotDTO currentCode = appService.getCurrentCode(userId, app.getId());
            AppVersionEntity createdVersion = appService.createVersionFromSnapshot(
                app.getId(),
                currentCode.htmlCode(),
                currentCode.jsCode(),
                message);

            relatedVersionIdRef.set(createdVersion.getId());

            String previousDir = openCodeWorkspaceService.getWorkspaceRelativeAppVersionDirectory(app.getId(),
                currentCode.version());
            String targetDir = openCodeWorkspaceService.getWorkspaceRelativeAppVersionDirectory(app.getId(),
                createdVersion.getVersionNumber());
            sendEditMessage(resolveOpenCodeSessionId(app), buildEditPrompt(history, message, previousDir, targetDir),
                app);

            return ServerSentEvent.<String>builder()
                .event("app_generated")
                .data(sseUtils.toAppGeneratedJson(app.getUuid(), createdVersion.getVersionNumber()))
                .build();
        }).subscribeOn(Schedulers.boundedElastic()).flux();

        return Flux.merge(chatStream, editStream)
            .doOnComplete(() -> saveAssistantEditMessage(
                userId,
                sessionId,
                chatBeforeEditBuffer.toString(),
                relatedAppIdRef.get(),
                relatedVersionIdRef.get()));
    }

        private AppEntity resolveRelatedApp(Long userId, String sessionId) {
        ChatSessionVO chatSession = chatSessionService.getSession(userId, sessionId);
        Long relatedAppId = chatSession.getRelatedAppId();
        if (relatedAppId == null) {
            throw new IllegalArgumentException("No related app found for current edit session");
        }

        appService.getAppById(userId, relatedAppId);
        return appService.getApp(relatedAppId);
    }

    private String resolveOpenCodeSessionId(AppEntity app) {
        if (app.getOpenCodeSessionId() != null && !app.getOpenCodeSessionId().isBlank()) {
            return app.getOpenCodeSessionId();
        }

        OpenCodeDtos.Session session = openCodeClient.createSession(app.getUuid());
        appService.bindOpenCodeSessionId(app.getId(), session.id());
        app.setOpenCodeSessionId(session.id());
        return session.id();
    }

    private void sendEditMessage(String openCodeSessionId, String prompt, AppEntity app) {
        OpenCodeDtos.MessageRequest request = OpenCodeDtos.MessageRequest.text(prompt);
        try {
            openCodeClient.sendMessage(openCodeSessionId, request);
        } catch (OpenCodeClientException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("-> 404")) {
                OpenCodeDtos.Session replacementSession = openCodeClient.createSession(app.getUuid());
                appService.bindOpenCodeSessionId(app.getId(), replacementSession.id());
                app.setOpenCodeSessionId(replacementSession.id());
                openCodeClient.sendMessage(replacementSession.id(), request);
                return;
            }
            throw exception;
        }
    }

    private String buildEditPrompt(String history, String message, String previousDir, String targetDir) {
        return openCodePromptService.buildAppEditPrompt(previousDir, targetDir)
                + "\n\nConversation history:\n" + safeHistory(history)
                + "\n\nCurrent user edit request:\n" + message
                + "\n\nPrevious version directory: " + previousDir
                + "\nCurrent target directory: " + targetDir;
    }

    private String safeHistory(String history) {
        return history == null || history.isBlank() ? "无历史对话。" : history;
    }

    private void saveAssistantEditMessage(
            Long userId,
            String sessionId,
            String chatBeforeEditContent,
            Long relatedAppId,
            Long relatedVersionId) {
        String content = chatBeforeEditContent == null ? "" : chatBeforeEditContent.trim();
        if (content.isBlank()) {
            log.warn("Skip saving empty EDIT assistant message for session {}", sessionId);
            return;
        }

        ChatMessageCreateDTO assistantMessageDto = new ChatMessageCreateDTO();
        assistantMessageDto.setSessionId(sessionId);
        assistantMessageDto.setRole("assistant");
        assistantMessageDto.setType("app");
        assistantMessageDto.setContent(content);
        assistantMessageDto.setRelatedAppId(relatedAppId);
        assistantMessageDto.setRelatedVersionId(relatedVersionId);
        chatMessageService.saveMessage(userId, assistantMessageDto);
        log.info("Saved EDIT assistant message for session {}, appId={}, versionId={}",
                sessionId,
                relatedAppId,
                relatedVersionId);
    }
}
