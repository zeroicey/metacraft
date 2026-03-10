package com.metacraft.api.modules.ai.service.pipeline;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.AppInfoExtractor;
import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.agent.CodeGenerator;
import com.metacraft.api.modules.ai.agent.PlanGenerator;
import com.metacraft.api.modules.ai.dto.AppCodeDTO;
import com.metacraft.api.modules.ai.dto.AppInfoDTO;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
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
        private final CodeGenerator codeGenerator;
        private final AppService appService;
        private final ImageService imageService;
        private final SseUtils sseUtils;
        private final ChatMessageService chatMessageService;
        private final ChatSessionService chatSessionService;

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

                Mono<AppEntity> appInfoMono = Mono.fromCallable(() -> {
                        AppInfoDTO info = appInfoExtractor.extract(message);
                        AppEntity app = appService.createApp(userId, info.getName(), info.getDescription());
                        chatSessionService.bindRelatedApp(userId, sessionId, app.getId());
                        relatedAppIdRef.set(app.getId());
                        return app;
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
                                appService.updateAppLogo(app.getId(), logoUuid + "." + ext);
                                return ServerSentEvent.<String>builder()
                                                .event("logo_generated")
                                                .data(sseUtils.toLogoGerneratedJson(logoUuid, ext))
                                                .build();
                        }).subscribeOn(Schedulers.boundedElastic()).flux();

                        // Placeholder for future code generation stream; keep it in the same stage as
                        // logo.
                        Mono<ServerSentEvent<String>> codeMono = Mono.fromCallable(() -> {
                                AppCodeDTO generatedCode = codeGenerator.generateCode(message, app.getName(),
                                                app.getDescription());
                                AppVersionEntity createdVersion = appService.createVersion(
                                                app.getId(),
                                                generatedCode.htmlCode(),
                                                generatedCode.jsCode(),
                                                "Initial version generated by AI");

                                relatedVersionIdRef.set(createdVersion.getId());

                                return ServerSentEvent.<String>builder()
                                                .event("app_generated")
                                                .data(sseUtils.toAppGeneratedJson(app.getUuid(),
                                                                createdVersion.getVersionNumber()))
                                                .build();
                        })
                                        .subscribeOn(Schedulers.boundedElastic());

                        return Flux.merge(logoMono, codeMono);

                });

                return Flux.merge(
                                chatStream,
                                planStream,
                                appInfoStream,
                                postAppInfoStream).doOnComplete(
                                                () -> saveAssistantGenMessage(
                                                                userId,
                                                                sessionId,
                                                                chatBeforeGenBuffer.toString(),
                                                                planBuffer.toString(),
                                                                relatedAppIdRef.get(),
                                                                relatedVersionIdRef.get()));
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
