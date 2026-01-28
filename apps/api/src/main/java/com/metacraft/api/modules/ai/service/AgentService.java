package com.metacraft.api.modules.ai.service;

import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.prompt.AgentPrompts;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    @Value("${zhipuai.chat.model:glm-4}")
    private String chatModel;

    @Value("${zhipuai.gen.model:glm-4}")
    private String genModel;

    @Value("${zhipuai.plan.model:glm-4}")
    private String planModel;

    private final AgentLLMService llmService;
    private final AgentIntentService intentService;
    private final AgentMessageService messageService;

    public SseEmitter chatStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = messageService.initSessionAndSaveUserMessage(request, userId);

        String fullSystemPrompt = AgentPrompts.SYSTEM + "\n\n" + AgentPrompts.CHAT;

        ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(fullSystemPrompt)
                .build();

        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(chatModel)
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(true)
                .temperature(0.7f)
                .maxTokens(4096)
                .build();

        llmService.executeStream(emitter, params, null, (fullContent) -> {
            messageService.saveAssistantMessage(userId, sessionId, fullContent);
        });
        return emitter;
    }

    public SseEmitter genStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = messageService.initSessionAndSaveUserMessage(request, userId);

        String fullSystemPrompt = AgentPrompts.SYSTEM + "\n\n" + AgentPrompts.GEN;

        ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(fullSystemPrompt)
                .build();

        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(genModel)
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(true)
                .temperature(0.2f)
                .maxTokens(4096)
                .build();

        llmService.executeStream(emitter, params, null, (fullContent) -> {
            messageService.handleGenCompletion(userId, sessionId, request.getMessage(), fullContent);
        });
        return emitter;
    }

    public SseEmitter planStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = messageService.initSessionAndSaveUserMessage(request, userId);

        String fullSystemPrompt = AgentPrompts.SYSTEM + "\n\n" + AgentPrompts.PLAN;

        ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(fullSystemPrompt)
                .build();

        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(planModel)
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(true)
                .temperature(0.5f)
                .maxTokens(2048)
                .build();

        llmService.executeStream(emitter, params, null, (fullContent) -> {
            messageService.saveAssistantMessage(userId, sessionId, fullContent);
        });
        return emitter;
    }

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
                llmService.executeStream(emitter, params, null, (fullContent) -> {
                    if ("gen".equals(intent)) {
                        messageService.handleGenCompletion(userId, sessionId, request.getMessage(), fullContent);
                    } else {
                        messageService.saveAssistantMessage(userId, sessionId, fullContent);
                    }
                });
                
            } catch (Exception e) {
                log.error("Unified stream error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
