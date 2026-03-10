package com.metacraft.api.modules.ai.service;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.IntentAnalyzer;
import com.metacraft.api.modules.ai.agent.SessionTitleGenerator;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO;
import com.metacraft.api.modules.ai.service.pipeline.AppEditPipelineService;
import com.metacraft.api.modules.ai.service.pipeline.AppGenPipelineService;
import com.metacraft.api.modules.ai.service.pipeline.ChatPipelineService;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.ai.vo.ChatSessionVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedOrchestrator {
    private static final int HISTORY_WINDOW_SIZE = 20;

    private final IntentAnalyzer intentAnalyzer;
    private final SessionTitleGenerator sessionTitleGenerator;
    private final SseUtils sseUtils;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    // 注入各个专门的流水线 Service
    private final ChatPipelineService chatPipelineService;
    private final AppGenPipelineService appGenPipelineService;
    private final AppEditPipelineService appEditPipelineService;

    public Flux<ServerSentEvent<String>> handleRequest(AgentRequestDTO request, Long userId) {
        ServerSentEvent<String> doneEvent = ServerSentEvent.<String>builder()
                .event("done")
                .data(sseUtils.toDoneJson())
                .build();

        return Mono.fromCallable(() -> prepareAndSaveUserMessage(request, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(context -> Mono.fromCallable(() -> intentAnalyzer.analyze(context.message()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMapMany(intent -> {
                            log.info("User {} intent {}", userId, intent);

                            ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                                    .event("intent")
                                    .data(sseUtils.toIntentJson(intent.name()))
                                    .build();

                            Flux<ServerSentEvent<String>> contentStream = switch (intent) {
                                case CHAT -> chatPipelineService.execute(context.message(), context.history(), userId,
                                        context.sessionId());
                                case GEN -> appGenPipelineService.execute(context.message(), context.history(), userId,
                                        context.sessionId());
                                case EDIT -> appEditPipelineService.execute(context.message());
                            };

                            return contentStream.startWith(intentEvent);
                        }))
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(sseUtils.toErrorJson(e.getMessage()))
                        .build()))
                .concatWithValues(doneEvent);
    }

    private static final String DEFAULT_SESSION_TITLE = "未命名会话";

    private RequestContext prepareAndSaveUserMessage(AgentRequestDTO request, Long userId) {
        String sessionId = resolveSessionId(request, userId);
        String history = chatMessageService.buildRecentConversationHistory(userId, sessionId, HISTORY_WINDOW_SIZE);

        // 如果会话标题是默认值，用第一条消息更新标题
        updateSessionTitleIfNeeded(userId, sessionId, request.getMessage());

        ChatMessageCreateDTO userMessageDto = new ChatMessageCreateDTO();
        userMessageDto.setSessionId(sessionId);
        userMessageDto.setRole("user");
        userMessageDto.setContent(request.getMessage());
        chatMessageService.saveMessage(userId, userMessageDto);
        log.info("Saved user message for session {}", sessionId);

        return new RequestContext(request.getMessage(), sessionId, history);
    }

    private void updateSessionTitleIfNeeded(Long userId, String sessionId, String firstMessage) {
        try {
            ChatSessionVO session = chatSessionService.getSession(userId, sessionId);
            if (DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
                log.info("Updating default session title for session {}", sessionId);
                ChatSessionUpdateDTO updateDto = new ChatSessionUpdateDTO();
                updateDto.setTitle(toSessionTitle(firstMessage));
                chatSessionService.updateSession(userId, sessionId, updateDto);
            }
        } catch (Exception e) {
            log.warn("Failed to update session title: {}", e.getMessage());
        }
    }

    private String resolveSessionId(AgentRequestDTO request, Long userId) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            String sessionTitle = toSessionTitle(request.getMessage());
            ChatSessionCreateDTO sessionDto = new ChatSessionCreateDTO();
            sessionDto.setTitle(sessionTitle);
            String createdSessionId = chatSessionService.createSession(userId, sessionDto).getSessionId();
            log.info("Created new session {} for user {}", createdSessionId, userId);
            return createdSessionId;
        }

        try {
            chatSessionService.getSession(userId, sessionId);
            log.info("Using existing session {} for user {}", sessionId, userId);
            return sessionId;
        } catch (IllegalArgumentException e) {
            log.warn("Session {} validation failed: {}, creating new session", sessionId, e.getMessage());
            String sessionTitle = toSessionTitle(request.getMessage());
            ChatSessionCreateDTO sessionDto = new ChatSessionCreateDTO();
            sessionDto.setTitle(sessionTitle);
            return chatSessionService.createSession(userId, sessionDto).getSessionId();
        }
    }

    private String toSessionTitle(String message) {
        try {
            String title = sessionTitleGenerator.generateTitle(message);
            if (title != null && !title.isBlank()) {
                log.info("Created new session {} for user {}", title, message);
                return title;
            }
        } catch (Exception e) {
            log.warn("Failed to generate session title with AI, using fallback: {}", e.getMessage());
        }
        // Fallback: 简单截断
        return message.length() > 50 ? message.substring(0, 47) + "..." : message;
    }

    private record RequestContext(String message, String sessionId, String history) {
    }
}