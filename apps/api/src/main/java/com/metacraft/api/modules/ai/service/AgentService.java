package com.metacraft.api.modules.ai.service;

import java.util.UUID;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.AppInfoExtractor;
import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.agent.IntentAnalyzer;
import com.metacraft.api.modules.ai.agent.PlanGenerator;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.entity.AppInfoEntity;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.app.service.ImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {
    private final AppInfoExtractor appInfoExtractor;
    private final IntentAnalyzer intentAnalyzer;
    private final PlanGenerator planGenerator;
    private final ImageService imageService;
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
                        case EDIT -> handleEditIntent();
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
        Flux<ServerSentEvent<String>> chatStream = chatAgent.chatBeforeGen(message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(sseUtils.toContentJson(chunk))
                        .build());

        Flux<ServerSentEvent<String>> planStream = planGenerator.generatePlan(message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("plan")
                        .data(sseUtils.toPlanJson(chunk))
                        .build());

        Mono<AppInfoEntity> appInfoMono = Mono.fromCallable(() -> appInfoExtractor.extract(message))
                .subscribeOn(Schedulers.boundedElastic()).cache();

        Flux<ServerSentEvent<String>> appInfoStream = appInfoMono.map(info -> ServerSentEvent.<String>builder()
                .event("app_info")
                .data(sseUtils.toAppInfoJson(info))
                .build()).flux();

        // logo/code generation must happen only after app info extraction is complete.
        Flux<ServerSentEvent<String>> logoAndCodeStream = appInfoMono.flatMapMany(info -> {
            Flux<ServerSentEvent<String>> logoStream = Mono.fromCallable(() -> {
                String logoUuid = UUID.randomUUID().toString();
                String ext = imageService.generateLogoAndSave(info, logoUuid);
                return ServerSentEvent.<String>builder()
                        .event("logo_generated")
                        .data(sseUtils.toLogoGerneratedJson(logoUuid, ext))
                        .build();
            }).subscribeOn(Schedulers.boundedElastic()).flux();

            // Placeholder for future code generation stream; keep it in the same stage as logo.
            Flux<ServerSentEvent<String>> codeStream = Flux.empty();

            return Flux.merge(logoStream, codeStream);
        });

        Flux<ServerSentEvent<String>> postAppInfoStream = Flux.concat(appInfoStream, logoAndCodeStream);


        return Flux.merge(
                chatStream,
                planStream,
                postAppInfoStream
        );
    }

    private Flux<ServerSentEvent<String>> handleEditIntent() {
        return Flux.empty();
    }
}
