package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Pattern APP_NAME_PATTERN = Pattern.compile("应用名称：([^\\r\\n]+)");
    private static final Pattern APP_DESCRIPTION_PATTERN = Pattern.compile("应用描述：([^\\r\\n]+)");
    private static final Pattern HTML_PATTERN = Pattern.compile("(?is)(<!doctype\\s+html.*?</html>)");

    private final AgentIntentService intentService;
    private final AgentAiService agentAiService;
    private final AppService appService;
    private final SseUtils sseUtils;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    public Flux<ServerSentEvent<String>> unified(AgentRequestDTO request, Long userId) {
        // AtomicReference to collect AI response content for saving later
        AtomicReference<String> aiResponseContent = new AtomicReference<>("");

        return Mono.fromCallable(() -> {
            // 1. Identify Intent
            AgentIntentRequestDTO intentReq = new AgentIntentRequestDTO();
            intentReq.setMessage(request.getMessage());
            return intentService.classifyIntent(intentReq);
        })
        .subscribeOn(Schedulers.boundedElastic()) // Run intent classification on blocking thread
        .flatMapMany(intent -> {
            log.info("User {} intent: {}", userId, intent);

            // 2. Handle session (create new or validate existing)
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                // Create new session with title from first message (truncate to 50 chars)
                String sessionTitle = request.getMessage().length() > 50
                    ? request.getMessage().substring(0, 47) + "..."
                    : request.getMessage();
                ChatSessionCreateDTO sessionDto = new ChatSessionCreateDTO();
                sessionDto.setTitle(sessionTitle);
                var sessionVO = chatSessionService.createSession(userId, sessionDto);
                sessionId = sessionVO.getSessionId();
                log.info("Created new session {} for user {}", sessionId, userId);
            } else {
                // Validate session exists and belongs to user
                try {
                    chatSessionService.getSession(userId, sessionId);
                    log.info("Using existing session {} for user {}", sessionId, userId);
                } catch (IllegalArgumentException e) {
                    log.warn("Session {} validation failed: {}, creating new session", sessionId, e.getMessage());
                    String sessionTitle = request.getMessage().length() > 50
                        ? request.getMessage().substring(0, 47) + "..."
                        : request.getMessage();
                    ChatSessionCreateDTO sessionDto = new ChatSessionCreateDTO();
                    sessionDto.setTitle(sessionTitle);
                    var sessionVO = chatSessionService.createSession(userId, sessionDto);
                    sessionId = sessionVO.getSessionId();
                }
            }

            // 3. Save user message
            ChatMessageCreateDTO userMessageDto = new ChatMessageCreateDTO();
            userMessageDto.setSessionId(sessionId);
            userMessageDto.setRole("user");
            userMessageDto.setContent(request.getMessage());
            chatMessageService.saveMessage(userId, userMessageDto);
            log.info("Saved user message for session {}", sessionId);

            // Send intent event first (JSON: {"intent":"chat"} or {"intent":"gen"})
            ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                    .event("intent")
                    .data(sseUtils.toIntentJson(intent))
                    .build();

            Flux<String> aiStream;
            String logoUuid = null;

            if ("gen".equalsIgnoreCase(intent)) {
                logoUuid = UUID.randomUUID().toString();
                // Pass userId to AI service for tool execution
                aiStream = agentAiService.generateApp(request.getMessage(), userId, logoUuid);
            } else {
                aiStream = agentAiService.chat(request.getMessage());
            }

            String eventType = "gen".equalsIgnoreCase(intent) ? "plan" : "message";

            // Stream AI content as plan/message events (JSON: {"content":"..."})
            // Also collect content for saving later
            Flux<ServerSentEvent<String>> messageStream = aiStream
                    .doOnNext(content -> {
                        // Append each chunk to the response content
                        aiResponseContent.updateAndGet(existing -> existing + content);
                    })
                    .map(content -> ServerSentEvent.<String>builder()
                            .event(eventType)
                            .data(sseUtils.toContentJson(content))
                            .build());

            // After stream ends, save AI message and check for new app
            String finalSessionId = sessionId;
            String finalLogoUuid = logoUuid;
            Flux<ServerSentEvent<String>> postStream = Flux.defer(() -> {
                // For gen mode, locate the app created in this request using unique logo UUID.
                AppEntity generatedApp = "gen".equalsIgnoreCase(intent)
                        ? appService.getLatestAppByUserIdAndLogoUuid(userId, finalLogoUuid)
                        : null;

                // Save AI assistant message
                String fullResponse = aiResponseContent.get();
                if (fullResponse != null && !fullResponse.isEmpty()) {
                    ChatMessageCreateDTO aiMessageDto = new ChatMessageCreateDTO();
                    aiMessageDto.setSessionId(finalSessionId);
                    aiMessageDto.setRole("assistant");
                    aiMessageDto.setContent(fullResponse);

                    // If gen mode and app was generated, bind app info to message
                    if ("gen".equalsIgnoreCase(intent) && generatedApp != null) {
                        aiMessageDto.setType("app");
                        aiMessageDto.setRelatedAppId(generatedApp.getId());
                        aiMessageDto.setRelatedVersionId(generatedApp.getCurrentVersionId());
                        log.info("Gen mode: binding app {} with current version record {} to AI message",
                            generatedApp.getId(), generatedApp.getCurrentVersionId());
                    }

                    chatMessageService.saveMessage(userId, aiMessageDto);
                    log.info("Saved AI response for session {}, content length: {}", finalSessionId, fullResponse.length());
                }

                if ("gen".equalsIgnoreCase(intent) && generatedApp == null) {
                    generatedApp = recoverGeneratedAppFromResponse(userId, finalLogoUuid, fullResponse);
                    if (generatedApp != null) {
                        log.warn("Recovered app creation from streamed content for user {}, appId={}", userId, generatedApp.getId());
                    }
                }

                if ("gen".equalsIgnoreCase(intent) && generatedApp == null) {
                    log.warn("Gen mode finished but no app is bound to logoUuid={}, userId={}", finalLogoUuid, userId);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(sseUtils.toErrorJson("本次生成未成功保存应用，请重试"))
                                    .build()
                    );
                }

                if (generatedApp != null) {
                    String previewUrl = "/api/preview/" + generatedApp.getUuid();
                    String logoUrl = finalLogoUuid != null ? "/api/logo/" + finalLogoUuid : null;
                    log.info("Gen mode completed, sending app_generated event with URL: {}, name: {}", previewUrl, generatedApp.getName());

                    // Update session with related app
                    com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO updateDto =
                        new com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO();
                    updateDto.setTitle(generatedApp.getName());
                    chatSessionService.updateSession(userId, finalSessionId, updateDto);

                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("app_generated")
                                    .data(sseUtils.toAppGeneratedJson(previewUrl, generatedApp.getUuid(), generatedApp.getName(), generatedApp.getDescription(), logoUrl))
                                    .build()
                    );
                }
                return Flux.empty();
            });

            // Send done event at the end (JSON: {})
            Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("done")
                            .data(sseUtils.toDoneJson())
                            .build()
            );

            return Flux.concat(Flux.just(intentEvent), messageStream, postStream, doneEvent)
                    .onErrorResume(e -> {
                        log.error("Unified agent stream failed for user {}", userId, e);
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(sseUtils.toErrorJson("生成流程失败，请重试"))
                                        .build(),
                                ServerSentEvent.<String>builder()
                                        .event("done")
                                        .data(sseUtils.toDoneJson())
                                        .build()
                        );
                    });
        });
    }

    private AppEntity recoverGeneratedAppFromResponse(Long userId, String logoUuid, String fullResponse) {
        if (fullResponse == null || fullResponse.isBlank() || logoUuid == null || logoUuid.isBlank()) {
            return null;
        }

        String html = extractFirstGroup(HTML_PATTERN, fullResponse).orElse(null);
        if (html == null || html.isBlank()) {
            return null;
        }

        String name = extractFirstGroup(APP_NAME_PATTERN, fullResponse).orElse("未命名应用").trim();
        String description = extractFirstGroup(APP_DESCRIPTION_PATTERN, fullResponse).orElse("AI 生成应用").trim();

        try {
            AppEntity app = appService.createApp(userId, name, description);
            appService.updateAppLogo(app.getId(), logoUuid + ".png");
            appService.createVersion(app.getId(), html, "Recovered from streamed generation");
            return appService.getApp(app.getId());
        } catch (Exception e) {
            log.error("Failed to recover app from streamed content for user {}", userId, e);
            return null;
        }
    }

    private Optional<String> extractFirstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.ofNullable(matcher.group(1));
    }
}
