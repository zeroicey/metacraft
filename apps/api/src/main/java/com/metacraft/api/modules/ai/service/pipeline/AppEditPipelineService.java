package com.metacraft.api.modules.ai.service.pipeline;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.agent.CodeEditor;
import com.metacraft.api.modules.ai.dto.AppCodeDiffDTO;
import com.metacraft.api.modules.ai.dto.AppCodeSnapshotDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.service.AppCodeDiffService;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.ai.vo.ChatSessionVO;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.service.AppService;
import com.metacraft.api.modules.app.vo.AppVO;

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
    private final CodeEditor codeEditor;
    private final AppService appService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final AppCodeDiffService appCodeDiffService;
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

        Mono<AppVO> appMono = Mono.fromCallable(() -> resolveRelatedApp(userId, sessionId))
                .subscribeOn(Schedulers.boundedElastic())
                .cache();

        Flux<ServerSentEvent<String>> editStream = appMono.flatMapMany(app -> Mono.fromCallable(() -> {
            relatedAppIdRef.set(app.getId());

            AppCodeSnapshotDTO currentCode = appService.getCurrentCode(userId, app.getId());
            AppCodeDiffDTO diff = codeEditor.editCode(message, currentCode.htmlCode(), currentCode.jsCode());

            String updatedHtml = appCodeDiffService.applyDiff(currentCode.htmlCode(), diff.htmlDiff(), "HTML");
            String updatedJs = appCodeDiffService.applyDiff(currentCode.jsCode(), diff.jsDiff(), "JavaScript");

            if (sameContent(currentCode.htmlCode(), updatedHtml) && sameContent(currentCode.jsCode(), updatedJs)) {
                relatedVersionIdRef.set(currentCode.versionId());
                return ServerSentEvent.<String>builder()
                        .event("app_generated")
                        .data(sseUtils.toAppGeneratedJson(app.getUuid(), currentCode.version()))
                        .build();
            }

            AppVersionEntity createdVersion = appService.createVersion(app.getId(), updatedHtml, updatedJs, message);
            relatedVersionIdRef.set(createdVersion.getId());
            return ServerSentEvent.<String>builder()
                    .event("app_generated")
                    .data(sseUtils.toAppGeneratedJson(app.getUuid(), createdVersion.getVersionNumber()))
                    .build();
        }).subscribeOn(Schedulers.boundedElastic()));

        return Flux.merge(chatStream, editStream)
                .doOnComplete(() -> saveAssistantEditMessage(
                        userId,
                        sessionId,
                        chatBeforeEditBuffer.toString(),
                        relatedAppIdRef.get(),
                        relatedVersionIdRef.get()));
    }

    private AppVO resolveRelatedApp(Long userId, String sessionId) {
        ChatSessionVO session = chatSessionService.getSession(userId, sessionId);
        Long relatedAppId = session.getRelatedAppId();
        if (relatedAppId == null) {
            throw new IllegalArgumentException("No related app found for current edit session");
        }

        return appService.getAppById(userId, relatedAppId);
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

    private boolean sameContent(String first, String second) {
        return normalizeNewlines(first).equals(normalizeNewlines(second));
    }

    private String normalizeNewlines(String content) {
        return content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
    }
}
