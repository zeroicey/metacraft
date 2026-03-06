package com.metacraft.api.modules.ai.service;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.ChatAgent;
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
public class AgentService {
    private final IntentAnalyzer intentAnalyzer;
    private final ChatAgent chatAgent;
    private final SseUtils sseUtils;

    public Flux<ServerSentEvent<String>> handleRequest(AgentRequestDTO request, Long userId) {
        return Mono.fromCallable(() -> {
            return intentAnalyzer.analyze(request.getMessage());
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(intent -> {
                    log.info("User {} intent {}", userId, intent);

                    ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                            .event("intent")
                            .data(sseUtils.toIntentJson(intent.name()))
                            .build();
                    Flux<ServerSentEvent<String>> contentStream = switch (intent) {
                        case CHAT -> handleChatIntent(request.getMessage());
                        case GEN -> handleGenIntent(request.getMessage());
                        case EDIT -> handleEditIntent(request.getMessage());
                    };
                    return contentStream.startWith(intentEvent);
                })
                .onErrorResume(e -> {
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(sseUtils.toErrorJson(e.getMessage()))
                            .build());
                });

    }

    private Flux<ServerSentEvent<String>> handleChatIntent(String message) {
        return chatAgent.chat(message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(sseUtils.toContentJson(chunk))
                        .build());
    }

    private Flux<ServerSentEvent<String>> handleGenIntent(String message) {
        return Flux.just(ServerSentEvent.<String>builder()
                .event("message")
                .data("生成功能还在开发中...")
                .build());
    }

    private Flux<ServerSentEvent<String>> handleEditIntent(String message) {
        return Flux.empty();
    }
}
