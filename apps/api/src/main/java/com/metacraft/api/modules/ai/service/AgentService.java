package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules.ai.util.SseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Pattern APP_NAME_PATTERN = Pattern.compile("应用名称：([^\\r\\n]+)");
    private static final Pattern APP_DESCRIPTION_PATTERN = Pattern.compile("应用描述：([^\\r\\n]+)");
    private static final String HTML_START_MARKER = "<!doctype html";

    private final AgentIntentService intentService;
    private final AgentAiService agentAiService;
    private final SseUtils sseUtils;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final GenerationRunService generationRunService;
    private final GenerationTaskService generationTaskService;
    private final GenerationArtifactService generationArtifactService;
    private final RunSseEventService runSseEventService;

    public Flux<ServerSentEvent<String>> run(AgentRequestDTO request, Long userId, String runId) {
        AtomicReference<String> aiResponseContent = new AtomicReference<>("");
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicBoolean specEmitted = new AtomicBoolean(false);
        AtomicBoolean planDoneEmitted = new AtomicBoolean(false);
        AtomicBoolean codeMode = new AtomicBoolean(false);

        Flux<ServerSentEvent<String>> asyncEventStream = runSseEventService.subscribe(runId);

        generationTaskService.markRunning(runId, "intent");

        return Mono.fromCallable(() -> {
            AgentIntentRequestDTO intentReq = new AgentIntentRequestDTO();
            intentReq.setMessage(request.getMessage());
            return intentService.classifyIntent(intentReq);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(intent -> {
            generationRunService.markIntentDone(runId, intent);
            generationTaskService.markSuccess(runId, "intent");
            log.info("Run {} user {} intent: {}", runId, userId, intent);

            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                String sessionTitle = request.getMessage().length() > 50
                        ? request.getMessage().substring(0, 47) + "..."
                        : request.getMessage();
                ChatSessionCreateDTO sessionDto = new ChatSessionCreateDTO();
                sessionDto.setTitle(sessionTitle);
                var sessionVO = chatSessionService.createSession(userId, sessionDto);
                sessionId = sessionVO.getSessionId();
            } else {
                try {
                    chatSessionService.getSession(userId, sessionId);
                } catch (IllegalArgumentException e) {
                    String sessionTitle = request.getMessage().length() > 50
                            ? request.getMessage().substring(0, 47) + "..."
                            : request.getMessage();
                    ChatSessionCreateDTO sessionDto = new ChatSessionCreateDTO();
                    sessionDto.setTitle(sessionTitle);
                    var sessionVO = chatSessionService.createSession(userId, sessionDto);
                    sessionId = sessionVO.getSessionId();
                }
            }

            ChatMessageCreateDTO userMessageDto = new ChatMessageCreateDTO();
            userMessageDto.setSessionId(sessionId);
            userMessageDto.setRole("user");
            userMessageDto.setContent(request.getMessage());
            chatMessageService.saveMessage(userId, userMessageDto);

            ServerSentEvent<String> runStartedEvent = ServerSentEvent.<String>builder()
                    .event("run_started")
                    .data(sseUtils.toRunStartedJson(runId, sessionId))
                    .build();

            ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                    .event("intent")
                    .data(sseUtils.toIntentJson(runId, intent))
                    .build();

            Flux<String> aiStream;
            String logoUuid = null;
            if ("gen".equalsIgnoreCase(intent)) {
                logoUuid = UUID.randomUUID().toString();
                generationTaskService.markRunning(runId, "code");
                aiStream = agentAiService.generateApp(request.getMessage(), userId, logoUuid, runId);
            } else {
                aiStream = agentAiService.chat(request.getMessage());
            }

            Flux<ServerSentEvent<String>> messageStream = aiStream
                    .flatMap(content -> {
                        String merged = aiResponseContent.updateAndGet(existing -> existing + content);

                        if (!"gen".equalsIgnoreCase(intent)) {
                            return Flux.just(ServerSentEvent.<String>builder()
                                    .event("message_delta")
                                    .data(sseUtils.toContentJson(runId, content))
                                    .build());
                        }

                        Flux<ServerSentEvent<String>> specEvent = Flux.empty();
                        if (!specEmitted.get()) {
                            String name = extractFirstGroup(APP_NAME_PATTERN, merged);
                            String description = extractFirstGroup(APP_DESCRIPTION_PATTERN, merged);
                            if (name != null && description != null) {
                                specEmitted.set(true);
                                specEvent = Flux.just(ServerSentEvent.<String>builder()
                                        .event("spec_ready")
                                        .data(sseUtils.toSpecReadyJson(runId, name.trim(), description.trim()))
                                        .build());
                            }
                        }

                        boolean containsHtmlStart = merged.toLowerCase().contains(HTML_START_MARKER);
                        if (!codeMode.get() && containsHtmlStart) {
                            codeMode.set(true);
                        }

                        if (codeMode.get()) {
                            Flux<ServerSentEvent<String>> planDoneEvent = Flux.empty();
                            if (!planDoneEmitted.get()) {
                                planDoneEmitted.set(true);
                                planDoneEvent = Flux.just(ServerSentEvent.<String>builder()
                                        .event("plan_done")
                                        .data(sseUtils.toRunOnlyJson(runId))
                                        .build());
                            }
                            Flux<ServerSentEvent<String>> codeEvent = Flux.just(ServerSentEvent.<String>builder()
                                    .event("code_delta")
                                    .data(sseUtils.toContentJson(runId, content))
                                    .build());
                            return Flux.concat(specEvent, planDoneEvent, codeEvent);
                        }

                        Flux<ServerSentEvent<String>> planEvent = Flux.just(ServerSentEvent.<String>builder()
                                .event("plan_delta")
                                .data(sseUtils.toContentJson(runId, content))
                                .build());
                        return Flux.concat(specEvent, planEvent);
                    });

            String finalSessionId = sessionId;
            Flux<ServerSentEvent<String>> postStream = Flux.defer(() -> {
                String fullResponse = aiResponseContent.get();
                if (!"gen".equalsIgnoreCase(intent) && fullResponse != null && !fullResponse.isEmpty()) {
                    ChatMessageCreateDTO aiMessageDto = new ChatMessageCreateDTO();
                    aiMessageDto.setSessionId(finalSessionId);
                    aiMessageDto.setRole("assistant");
                    aiMessageDto.setContent(fullResponse);

                    chatMessageService.saveMessage(userId, aiMessageDto);
                }

                if (!"gen".equalsIgnoreCase(intent)) {
                    return Flux.empty();
                }

                if (!planDoneEmitted.get()) {
                    planDoneEmitted.set(true);
                }
                generationTaskService.markSuccess(runId, "code");
                var appArtifact = generationArtifactService.getLatestArtifactContent(runId, "app_saved");
                if (appArtifact.isEmpty()) {
                    failed.set(true);
                    generationRunService.markFailed(runId, "No app artifact persisted in generation run");
                    generationTaskService.markFailed(runId, "save", "No app_saved artifact for run");
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(sseUtils.toErrorJson(runId, "本次生成未成功保存应用，请重试"))
                                    .build()
                    );
                }

                Long appId = ((Number) appArtifact.get().getOrDefault("appId", 0)).longValue();
                Long versionId = ((Number) appArtifact.get().getOrDefault("versionId", 0)).longValue();
                String appUuid = String.valueOf(appArtifact.get().getOrDefault("appUuid", ""));
                String previewUrl = String.valueOf(appArtifact.get().getOrDefault("previewUrl", ""));
                String name = String.valueOf(appArtifact.get().getOrDefault("name", ""));
                String description = String.valueOf(appArtifact.get().getOrDefault("description", ""));
                String logoUrl = String.valueOf(appArtifact.get().getOrDefault("logoUrl", ""));

                ChatMessageCreateDTO appMessageDto = new ChatMessageCreateDTO();
                appMessageDto.setSessionId(finalSessionId);
                appMessageDto.setRole("assistant");
                appMessageDto.setContent(fullResponse);
                appMessageDto.setType("app");
                appMessageDto.setRelatedAppId(appId > 0 ? appId : null);
                appMessageDto.setRelatedVersionId(versionId > 0 ? versionId : null);
                chatMessageService.saveMessage(userId, appMessageDto);

                com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO updateDto =
                        new com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO();
                updateDto.setTitle(name);
                chatSessionService.updateSession(userId, finalSessionId, updateDto);

                return Flux.just(
                    ServerSentEvent.<String>builder()
                        .event("code_done")
                        .data(sseUtils.toRunOnlyJson(runId))
                        .build(),
                        ServerSentEvent.<String>builder()
                                .event("app_saved")
                                .data(sseUtils.toAppSavedJson(
                                        runId,
                                        appId > 0 ? appId : null,
                                        appUuid,
                                        versionId > 0 ? versionId : null,
                                        previewUrl,
                                        name,
                                        description,
                                        logoUrl
                                ))
                                .build()
                );
            });

            Flux<ServerSentEvent<String>> doneEvent = Flux.defer(() -> {
                if (!failed.get()) {
                    generationRunService.markSucceeded(runId, finalSessionId);
                }
                waitForLogoTerminalIfNeeded(runId, 4000L);
                return Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data(sseUtils.toDoneJson(runId))
                        .build());
            });

                Flux<ServerSentEvent<String>> primaryStream = Flux.concat(
                    Flux.just(runStartedEvent, intentEvent),
                    messageStream,
                    postStream,
                    doneEvent
                );

                return Flux.merge(primaryStream, asyncEventStream)
                    .doFinally(signalType -> runSseEventService.complete(runId))
                    .onErrorResume(e -> {
                        log.error("Run {} stream failed for user {}", runId, userId, e);
                        generationRunService.markFailed(runId, e.getMessage());
                        generationTaskService.markFailed(runId, "code", e.getMessage());
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(sseUtils.toErrorJson(runId, "生成流程失败，请重试"))
                                        .build(),
                                ServerSentEvent.<String>builder()
                                        .event("done")
                                        .data(sseUtils.toDoneJson(runId))
                                        .build()
                        );
                    });
        });
    }

    private void waitForLogoTerminalIfNeeded(String runId, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            var statusOpt = generationTaskService.getStatus(runId, "logo");
            if (statusOpt.isEmpty()) {
                return;
            }
            String status = statusOpt.get();
            if ("SUCCESS".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status) || "SKIPPED".equalsIgnoreCase(status)) {
                return;
            }
            try {
                Thread.sleep(120L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private String extractFirstGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
