package com.metacraft.api.modules.ai.service;

import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.prompt.AgentPrompts;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import com.metacraft.api.modules.app.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;

import com.metacraft.api.modules.ai.dto.AppMetadataDTO;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    @Value("${zhipuai.chat.model:glm-4}")
    private String chatModel;

    @Value("${zhipuai.gen.model:glm-4}")
    private String genModel;

    private final AgentLLMService llmService;
    private final AgentIntentService intentService;
    private final AgentMessageService messageService;
    private final AppService appService;
    private final ObjectMapper objectMapper;

    public SseEmitter unifiedStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 1. 初始化会话并保存用户消息
                String sessionId = messageService.initSessionAndSaveUserMessage(request, userId);
                
                // 2. 识别意图
                AgentIntentRequestDTO intentReq = new AgentIntentRequestDTO();
                intentReq.setMessage(request.getMessage());
                AgentIntentResponseVO intentVO = intentService.classifyIntent(intentReq);
                String intent = intentVO.getType();
                
                // 3. 发送意图事件给前端
                emitter.send(SseEmitter.event().name("intent").data(intent));
                
                // 4. 根据意图构建 Prompt 和参数
                String model;
                String systemPrompt;
                
                if ("gen".equals(intent)) {
                    model = genModel;
                    systemPrompt = AgentPrompts.SYSTEM + "\n\n" + AgentPrompts.GEN;
                } else {
                    model = chatModel;
                    systemPrompt = AgentPrompts.SYSTEM + "\n\n" + AgentPrompts.CHAT;
                }
                
                ChatMessage systemMessage = ChatMessage.builder()
                        .role(ChatMessageRole.SYSTEM.value())
                        .content(systemPrompt)
                        .build();

                ChatMessage userMessage = ChatMessage.builder()
                        .role(ChatMessageRole.USER.value())
                        .content(request.getMessage())
                        .build();

                ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                        .model(model)
                        .messages(Arrays.asList(systemMessage, userMessage))
                        .stream(true)
                        .temperature(intent.equals("gen") ? 0.2f : 0.7f)
                        .maxTokens(4096)
                        .build();
                        
                // 5. 执行流式生成
                if ("gen".equals(intent)) {
                    // Start async metadata generation
                    CompletableFuture<AppMetadataDTO> metadataFuture = CompletableFuture.supplyAsync(() -> 
                        generateAppMetadata(request.getMessage())
                    );

                    StringBuilder localBuffer = new StringBuilder();
                    java.util.concurrent.atomic.AtomicInteger sentLength = new java.util.concurrent.atomic.AtomicInteger(0);
                    java.util.concurrent.atomic.AtomicBoolean planFinished = new java.util.concurrent.atomic.AtomicBoolean(false);
                    String DELIMITER = "<<<<CODE_GENERATION>>>>";
                    int KEEP_COUNT = DELIMITER.length() - 1;

                    llmService.executeStreamWithCallback(params, 
                        chunk -> {
                            try {
                                if (!planFinished.get()) {
                                    localBuffer.append(chunk);
                                    int delimiterIndex = localBuffer.indexOf(DELIMITER);
                                    
                                    if (delimiterIndex != -1) {
                                        // Found delimiter
                                        planFinished.set(true);
                                        
                                        // Send remaining plan part
                                        if (delimiterIndex > sentLength.get()) {
                                            String contentToSend = localBuffer.substring(sentLength.get(), delimiterIndex);
                                            java.util.Map<String, String> data = new java.util.HashMap<>();
                                            data.put("content", contentToSend);
                                            emitter.send(SseEmitter.event().data(data).name("plan"));
                                        }
                                    } else {
                                        // Delimiter not found yet.
                                        // Send safe part, keep tail
                                        int safeEndIndex = localBuffer.length() - KEEP_COUNT;
                                        if (safeEndIndex > sentLength.get()) {
                                             String contentToSend = localBuffer.substring(sentLength.get(), safeEndIndex);
                                             java.util.Map<String, String> data = new java.util.HashMap<>();
                                             data.put("content", contentToSend);
                                             emitter.send(SseEmitter.event().data(data).name("plan"));
                                             sentLength.set(safeEndIndex);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("Stream error", e);
                                emitter.completeWithError(e);
                            }
                        }, 
                        fullContent -> {
                             try {
                                // Extract Code
                                String codePart = fullContent;
                                int delimiterIndex = fullContent.lastIndexOf(DELIMITER);
                                if (delimiterIndex != -1) {
                                    codePart = fullContent.substring(delimiterIndex + DELIMITER.length());
                                }
                                codePart = codePart.trim();
                                
                                com.metacraft.api.modules.app.entity.AppVersionEntity version = messageService.handleGenCompletion(
                                    userId, 
                                    sessionId, 
                                    request.getMessage(), 
                                    codePart,
                                    metadataFuture
                                );
                                
                                if (version != null) {
                                    com.metacraft.api.modules.app.entity.AppEntity app = appService.getApp(version.getAppId());
                                    String previewUrl = "/api/preview/" + app.getUuid() + "/v/" + version.getVersionNumber();
                                    
                                    emitter.send(SseEmitter.event().name("app_generated").data(previewUrl));
                                }
                                
                                emitter.send(SseEmitter.event().data("[DONE]").name("done"));
                                emitter.complete();
                             } catch (Exception e) {
                                 log.error("Completion error", e);
                                 emitter.completeWithError(e);
                             }
                        },
                        error -> {
                            log.error("Stream error", error);
                            emitter.completeWithError(error);
                        }
                    );
                } else {
                    llmService.executeStream(emitter, params, null, (fullContent) -> messageService.saveAssistantMessage(userId, sessionId, fullContent));
                }
                
            } catch (Exception e) {
                log.error("Unified stream error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    private AppMetadataDTO generateAppMetadata(String userPrompt) {
        try {
            String systemPrompt = AgentPrompts.METADATA_GEN;
            
            ChatMessage systemMsg = ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM.value())
                    .content(systemPrompt)
                    .build();
            
            ChatMessage userMsg = ChatMessage.builder()
                    .role(ChatMessageRole.USER.value())
                    .content(userPrompt)
                    .build();

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(chatModel) // Use chat model for faster/cheaper metadata gen
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .temperature(0.7f)
                    .maxTokens(500)
                    .build();

            ai.z.openapi.service.model.ChatCompletionResponse response = llmService.executeSync(params);
            
            if (response.getData() != null && response.getData().getChoices() != null && !response.getData().getChoices().isEmpty()) {
                String content = response.getData().getChoices().get(0).getMessage().getContent().toString();
                // Clean content if it has Markdown
                if (content.startsWith("```json")) {
                    content = content.substring(7);
                } else if (content.startsWith("```")) {
                    content = content.substring(3);
                }
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
                content = content.trim();
                
                return objectMapper.readValue(content, AppMetadataDTO.class);
            }
        } catch (Exception e) {
            log.error("Failed to generate app metadata", e);
        }
        return null;
    }
}
