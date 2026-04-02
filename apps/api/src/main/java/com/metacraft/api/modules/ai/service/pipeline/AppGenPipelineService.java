package com.metacraft.api.modules.ai.service.pipeline;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.ai.agent.AppInfoExtractor;
import com.metacraft.api.modules.ai.agent.ArchitectAgent;
import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.agent.CodeFileAgent;
import com.metacraft.api.modules.ai.agent.CodeGenerator;
import com.metacraft.api.modules.ai.agent.ContractAgent;
import com.metacraft.api.modules.ai.agent.PlanGenerator;
import com.metacraft.api.modules.ai.dto.AppCodeDTO;
import com.metacraft.api.modules.ai.dto.AppInfoDTO;
import com.metacraft.api.modules.ai.dto.Blueprint;
import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.dto.CodeFileDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
import com.metacraft.api.modules.ai.service.TemplateFileService;
import com.metacraft.api.modules.ai.service.TemplateMatcherService;
import com.metacraft.api.modules.ai.util.SseUtils;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.service.AppService;
import com.metacraft.api.modules.app.service.ImageService;
import com.metacraft.api.modules.storage.service.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppGenPipelineService {

        @Value("${app.templates.matched-response-delay-ms:3000}")
        private long templateMatchedResponseDelayMs;

        @Value("${app.storage.path:apps}")
        private String appStoragePath;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private final ChatAgent chatAgent;
        private final PlanGenerator planGenerator;
        private final AppInfoExtractor appInfoExtractor;
        private final CodeGenerator codeGenerator;
        private final AppService appService;
        private final ImageService imageService;
        private final SseUtils sseUtils;
        private final ChatMessageService chatMessageService;
        private final ChatSessionService chatSessionService;
        private final TemplateMatcherService templateMatcherService;
        private final TemplateFileService templateFileService;
        private final ArchitectAgent architectAgent;
        private final ContractAgent contractAgent;
        private final CodeFileAgent codeFileAgent;
        private final StorageService storageService;

        public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId, boolean generateLogo) {
                StringBuffer chatBeforeGenBuffer = new StringBuffer();
                StringBuffer planBuffer = new StringBuffer();
                AtomicReference<Long> relatedAppIdRef = new AtomicReference<>();
                AtomicReference<Long> relatedVersionIdRef = new AtomicReference<>();
                AtomicReference<String> matchedTemplateRef = new AtomicReference<>();

                // Template matching - 先执行模板匹配，等待结果
                Mono<String> templateMatchMono = templateMatcherService.matchTemplate(message)
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnNext(matchedTemplateRef::set)
                                .cache();

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
                // 修改：让 codeStream 等待 templateMatchMono 完成后再执行
                Flux<ServerSentEvent<String>> postAppInfoStream = templateMatchMono
                                .thenMany(appInfoMono.flatMapMany(app -> {
                                        // Logo 生成：根据 generateLogo 参数决定是否生成
                                        Flux<ServerSentEvent<String>> logoMono;
                                        if (generateLogo) {
                                                logoMono = Mono.fromCallable(() -> {
                                                        String logoUuid = UUID.randomUUID().toString();
                                                        String ext = imageService.generateLogoAndSave(app, logoUuid);
                                                        appService.updateAppLogo(app.getId(), logoUuid + "." + ext);
                                                        return ServerSentEvent.<String>builder()
                                                                        .event("logo_generated")
                                                                        .data(sseUtils.toLogoGerneratedJson(logoUuid, ext))
                                                                        .build();
                                                }).subscribeOn(Schedulers.boundedElastic()).flux();
                                        } else {
                                                log.info("Skipping logo generation as requested");
                                                logoMono = Flux.empty();
                                        }

                                        Mono<ServerSentEvent<String>> codeStream = Mono.fromCallable(() -> {
                                                // Check if template matched
                                                String matchedTemplate = matchedTemplateRef.get();
                                                AppVersionEntity createdVersion;

                                                if (matchedTemplate != null) {
                                                        // Use template - copy files
                                                        log.info("Using template: {}", matchedTemplate);
                                                        createdVersion = createVersionFromTemplate(app, matchedTemplate);
                                                } else {
                                                        // Multi-stage Agent pipeline: Architect -> Contract -> CodeFile
                                                        log.info("Running multi-stage Agent pipeline for app: {}", app.getId());
                                                        createdVersion = generateCodeWithAgentPipeline(app, message);
                                                }

                                                relatedVersionIdRef.set(createdVersion.getId());

                                                return ServerSentEvent.<String>builder()
                                                                .event("app_generated")
                                                                .data(sseUtils.toAppGeneratedJson(app.getUuid(),
                                                                                createdVersion.getVersionNumber()))
                                                                .build();
                                        }).subscribeOn(Schedulers.boundedElastic())
                                                        .delayElement(resolveTemplateResponseDelay(matchedTemplateRef.get()));

                                        return Flux.merge(logoMono, codeStream);

                                }));

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

        private AppVersionEntity createVersionFromTemplate(AppEntity app, String templateName) {
                // First create an empty version
                AppVersionEntity version = appService.createVersion(
                                app.getId(),
                                "", // Empty content, will be copied from template
                                "",
                                "Created from template: " + templateName);

                // Copy template files
                boolean success = templateFileService.copyTemplateFiles(templateName, app.getId(),
                                version.getVersionNumber());

                if (!success) {
                        log.warn("Failed to copy template files for template: {}", templateName);
                } else {
                        log.info("Created app {} version {} from template {}", app.getId(), version.getVersionNumber(),
                                        templateName);
                }

                return version;
        }

        private AppVersionEntity generateCodeWithCodeGenerator(AppEntity app, String message) {
                AppCodeDTO generatedCode = codeGenerator.generateCode(message, app.getName(),
                                app.getDescription());
                return appService.createVersion(
                                app.getId(),
                                generatedCode.htmlCode(),
                                generatedCode.jsCode(),
                                "Initial version generated by AI");
        }

        private AppVersionEntity generateCodeWithAgentPipeline(AppEntity app, String message) {
                try {
                        // Step 1: Architect generates Blueprint
                        Blueprint blueprint = architectAgent.generateBlueprint(
                                message,
                                app.getName(),
                                app.getDescription());
                        String blueprintJson = objectMapper.writeValueAsString(blueprint);
                        log.info("Generated blueprint with {} files", blueprint.projectBlueprint().fileList().size());

                        // Step 2: Contract generates contract from blueprint
                        String contract = contractAgent.generateContract(blueprintJson);
                        log.info("Generated contract");

                        // Step 3: Create version first
                        AppVersionEntity version = appService.createVersion(
                                app.getId(),
                                "", // Empty content, will be populated from code files
                                "",
                                "Generated via Agent pipeline");

                        // Step 4: Generate code files in parallel and save
                        List<CodeFileDTO> codeFiles;
                        try {
                                codeFiles = blueprint.projectBlueprint().fileList().stream()
                                        .map(fileInfo -> {
                                                String fileInfoJson;
                                                try {
                                                        fileInfoJson = String.format(
                                                                "{\"file_id\":\"%s\",\"file_path\":\"%s\",\"purpose\":\"%s\",\"depends_on\":%s}",
                                                                fileInfo.fileId(),
                                                                fileInfo.filePath(),
                                                                fileInfo.purpose(),
                                                                objectMapper.writeValueAsString(fileInfo.dependsOn()));
                                                } catch (JsonProcessingException e) {
                                                        throw new RuntimeException("Failed to serialize fileInfo", e);
                                                }
                                                String code = codeFileAgent.generateCodeFile(fileInfoJson, contract);
                                                return new CodeFileDTO(fileInfo.fileId(), fileInfo.filePath(), code);
                                        })
                                        .toList();
                        } catch (RuntimeException e) {
                                log.error("Failed to generate code files", e);
                                throw e;
                        }

                        // Save all code files
                        saveCodeFiles(app.getId(), version.getVersionNumber(), codeFiles);
                        log.info("Saved {} code files for app {} version {}",
                                codeFiles.size(), app.getId(), version.getVersionNumber());

                        return version;
                } catch (JsonProcessingException e) {
                        log.error("Failed to serialize/deserialize JSON in Agent pipeline", e);
                        // Fallback to CodeGenerator
                        log.info("Falling back to CodeGenerator due to Agent pipeline error");
                        return generateCodeWithCodeGenerator(app, message);
                }
        }

        private Duration resolveTemplateResponseDelay(String matchedTemplate) {
                if (matchedTemplate == null || matchedTemplate.isBlank()) {
                        return Duration.ZERO;
                }
                if (templateMatchedResponseDelayMs <= 0) {
                        return Duration.ZERO;
                }
                return Duration.ofMillis(templateMatchedResponseDelayMs);
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

        // SSE event builder methods for multi-stage pipeline
        private ServerSentEvent<String> buildBlueprintEvent(String blueprintJson) {
                return ServerSentEvent.<String>builder()
                        .event("blueprint")
                        .data(blueprintJson)
                        .build();
        }

        private ServerSentEvent<String> buildContractEvent(String contract) {
                return ServerSentEvent.<String>builder()
                        .event("contract")
                        .data(contract)
                        .build();
        }

        private ServerSentEvent<String> buildCodeFileEvent(CodeFileDTO codeFile) {
                String json = String.format(
                        "{\"file_id\":\"%s\",\"file_path\":\"%s\",\"code\":\"%s\"}",
                        escapeJson(codeFile.fileId()),
                        escapeJson(codeFile.filePath()),
                        escapeJson(codeFile.code()));
                return ServerSentEvent.<String>builder()
                        .event("code_file")
                        .data(json)
                        .build();
        }

        private String escapeJson(String s) {
                if (s == null) return "";
                return s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
        }

        private void saveCodeFiles(Long appId, int versionNumber, List<CodeFileDTO> files) {
                String basePath = appStoragePath + "/" + appId + "/v" + versionNumber;
                for (CodeFileDTO file : files) {
                        String fullPath = basePath + "/" + file.filePath();
                        storageService.saveTextFile(fullPath, file.code());
                        log.info("Saved code file: {}", fullPath);
                }
        }
}
