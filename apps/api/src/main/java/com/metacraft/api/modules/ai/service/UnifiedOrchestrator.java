package com.metacraft.api.modules.ai.service;

import java.util.UUID;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.AppInfoExtractor;
import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.agent.CodeGenerator;
import com.metacraft.api.modules.ai.agent.IntentAnalyzer;
import com.metacraft.api.modules.ai.agent.PlanGenerator;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.AppCodeDTO;
import com.metacraft.api.modules.ai.dto.AppInfoDTO;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.app.entity.AppEntity;
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
public class UnifiedOrchestrator {
    private final AppInfoExtractor appInfoExtractor;
    private final IntentAnalyzer intentAnalyzer;
    private final PlanGenerator planGenerator;
    private final CodeGenerator codeGenerator;

    private final ImageService imageService;
    private final AppService appService;

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
                        case GEN -> handleGenIntent(request.getMessage(), userId);
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

    private Flux<ServerSentEvent<String>> handleGenIntent(String message, Long userId) {
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

        Mono<AppEntity> appInfoMono = Mono.fromCallable(() -> {
                    AppInfoDTO info = appInfoExtractor.extract(message);
                    return appService.createApp(userId, info.getName(), info.getDescription());
                })
                .subscribeOn(Schedulers.boundedElastic()).cache();

        Flux<ServerSentEvent<String>> appInfoStream = appInfoMono.map(app -> ServerSentEvent.<String>builder()
                .event("app_info")
                .data(sseUtils.toAppInfoJson(app.getName(), app.getDescription()))
                .build()).flux();

        // logo/code generation must happen only after app info extraction is complete.
        Flux<ServerSentEvent<String>> postAppInfoStream = appInfoMono.flatMapMany(app -> {
            Flux<ServerSentEvent<String>> logoMono = Mono.fromCallable(() -> {
                String logoUuid = UUID.randomUUID().toString();
                String ext = imageService.generateLogoAndSave(app, logoUuid);
                return ServerSentEvent.<String>builder()
                        .event("logo_generated")
                        .data(sseUtils.toLogoGerneratedJson(logoUuid, ext))
                        .build();
            }).subscribeOn(Schedulers.boundedElastic()).flux();

            // Placeholder for future code generation stream; keep it in the same stage as logo.
            Mono<ServerSentEvent<String>> codeMono = Mono.fromCallable(() -> {
                AppCodeDTO generatedCode = codeGenerator.generateCode(message, app.getName(), app.getDescription());
                appService.createVersion(app.getId(), generatedCode.htmlCode(), generatedCode.jsCode(), message);

                return app.getUuid();
            })
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(appUUID -> ServerSentEvent.<String>builder()
                            .event("app_generated")
                            .data(sseUtils.toAppGeneratedJson(appUUID)).build());

            return Flux.merge(logoMono, codeMono);
        });

        return Flux.merge(
                chatStream,
                planStream,
                appInfoStream,
                postAppInfoStream
        );
    }

    private Flux<ServerSentEvent<String>> handleEditIntent() {
        return Flux.empty();
    }
}
