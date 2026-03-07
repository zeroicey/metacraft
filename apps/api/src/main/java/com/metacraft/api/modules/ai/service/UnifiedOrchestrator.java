package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.service.pipeline.AppEditPipelineService;
import com.metacraft.api.modules.ai.service.pipeline.AppGenPipelineService;
import com.metacraft.api.modules.ai.service.pipeline.ChatPipelineService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.IntentAnalyzer;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.util.SseUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedOrchestrator {
    private final IntentAnalyzer intentAnalyzer;
    private final SseUtils sseUtils;

    // 注入各个专门的流水线 Service
    private final ChatPipelineService chatPipelineService;
    private final AppGenPipelineService appGenPipelineService;
    private final AppEditPipelineService appEditPipelineService;

    public Flux<ServerSentEvent<String>> handleRequest(AgentRequestDTO request, Long userId) {
        String message = request.getMessage();

        return Mono.fromCallable(() -> intentAnalyzer.analyze(message))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(intent -> {
                    log.info("User {} intent {}", userId, intent);

                    ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                            .event("intent")
                            .data(sseUtils.toIntentJson(intent.name()))
                            .build();

                    Flux<ServerSentEvent<String>> contentStream = switch (intent) {
                        case CHAT -> chatPipelineService.execute(message);
                        case GEN  -> appGenPipelineService.execute(message, userId);
                        case EDIT -> appEditPipelineService.execute(message);
                    };

                    return contentStream.startWith(intentEvent);
                })
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(sseUtils.toErrorJson(e.getMessage()))
                        .build()));
    }
}