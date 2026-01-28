package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.entity.ChatSessionEntity;
import com.metacraft.api.modules.ai.prompt.AgentPrompts;
import com.metacraft.api.modules.ai.repository.ChatMessageRepository;
import com.metacraft.api.modules.ai.repository.ChatSessionRepository;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;

@Service
public class AgentService {

    @Value("${zhipuai.api-key}")
    private String apiKey;

    @Value("${zhipuai.chat.model:glm-4}")
    private String chatModel;

    @Value("${zhipuai.gen.model:glm-4}")
    private String genModel;

    @Value("${zhipuai.intent.model:glm-4}")
    private String intentModel;

    @Value("${zhipuai.plan.model:glm-4}")
    private String planModel;

    private ZhipuAiClient client;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

    public AgentService(ChatMessageRepository chatMessageRepository, ChatSessionRepository chatSessionRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    @PostConstruct
    public void init() {
        this.client = ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .enableTokenCache()
                .build();
    }
    /**
     * 通用流式执行器
     *
     * @param params          Zhipu 请求参数
     * @param onRealTimeChunk 实时回调(可选)
     * @param onComplete      完成回调(可选)
     */
    private void executeStream(
            SseEmitter emitter,
            ChatCompletionCreateParams params,
            java.util.function.Consumer<String> onRealTimeChunk,
            java.util.function.Consumer<String> onComplete
    ) {
        StringBuilder accumulator = new StringBuilder();

        client.chat().createChatCompletion(params).getFlowable()
                .subscribe(
                        data -> {
                            if (data.getChoices() != null && !data.getChoices().isEmpty()) {
                                Delta delta = data.getChoices().get(0).getDelta();
                                if (delta != null && delta.getContent() != null) {
                                    String content = delta.getContent();
                                    if (!content.isEmpty()) {
                                        try {
                                            // 1. 发送给前端
                                            emitter.send(SseEmitter.event()
                                                    .data(content)
                                                    .name("message"));

                                            // 2. 拼接到缓存
                                            accumulator.append(content);

                                            // 3. 实时回调
                                            if (onRealTimeChunk != null) {
                                                onRealTimeChunk.accept(content);
                                            }
                                        } catch (IOException e) {
                                            emitter.completeWithError(e);
                                        }
                                    }
                                }
                            }
                        },
                        error -> {
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().data("[DONE]").name("done"));
                                emitter.complete();

                                // 4. 完成回调
                                if (onComplete != null) {
                                    onComplete.accept(accumulator.toString());
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
    }

    public SseEmitter chatStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = initSessionAndSaveUserMessage(request, userId);

        String fullSystemPrompt = AgentPrompts.SYSTEM + "\n\n" + AgentPrompts.CHAT;

        ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(fullSystemPrompt)
                .build();

        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        // TODO: 如果需要上下文，这里应该查询 chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        // 并构建历史消息列表。目前仅保存，暂不回填历史记录（根据用户需求）。

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(chatModel)
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(true)
                .temperature(0.7f)
                .maxTokens(4096)
                .build();

        executeStream(emitter, params, null, (fullContent) -> {
            saveAssistantMessage(userId, sessionId, fullContent);
        });
        return emitter;
    }

    public SseEmitter genStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = initSessionAndSaveUserMessage(request, userId);

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

        executeStream(emitter, params, null, (fullContent) -> {
            saveAssistantMessage(userId, sessionId, fullContent);
        });
        return emitter;
    }

    public SseEmitter planStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = initSessionAndSaveUserMessage(request, userId);

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
                .model(planModel) // Plan 通常使用推理能力较强的模型，这里暂时复用 chatModel
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(true)
                .temperature(0.5f)
                .maxTokens(2048)
                .build();

        String finalSessionId = sessionId;
        executeStream(emitter, params, null, (fullContent) -> {
            saveAssistantMessage(userId, finalSessionId, fullContent);
        });
        return emitter;
    }

    public SseEmitter unifiedStream(AgentRequestDTO request, Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 1. 初始化会话并保存用户消息
                String sessionId = initSessionAndSaveUserMessage(request, userId);
                
                // 2. 识别意图
                AgentIntentRequestDTO intentReq = new AgentIntentRequestDTO();
                intentReq.setMessage(request.getMessage());
                // 注意：这里调用 classifyIntent 是同步阻塞的，这正是我们放在异步线程的原因
                AgentIntentResponseVO intentVO = classifyIntent(intentReq);
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
                executeStream(emitter, params, null, (fullContent) -> {
                    saveAssistantMessage(userId, sessionId, fullContent);
                });
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    public AgentIntentResponseVO classifyIntent(AgentIntentRequestDTO request) {
        // 意图识别不需要全局背景，只需要严格的指令
        ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM.value())
                .content(AgentPrompts.INTENT)
                .build();
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(intentModel)
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(false)
                .temperature(0.01f) // 极低温度保证确定性
                .maxTokens(10)
                .build();

        ChatCompletionResponse response = client.chat().createChatCompletion(params);
        if (!response.isSuccess()) {
            throw new RuntimeException("意图识别失败: " + response.getMsg());
        }

        String text = response.getData().getChoices().get(0).getMessage().getContent().toString().trim().toLowerCase();
        // 简单清洗，防止模型输出 extra text
        if (text.contains("gen")) {
            text = "gen";
        } else {
            text = "chat";
        }

        String model = response.getData().getModel();
        return new AgentIntentResponseVO(text, model);
    }

    private String initSessionAndSaveUserMessage(AgentRequestDTO request, Long userId) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString();
            
            // 创建新会话
            String title = request.getMessage().length() > 20 ? request.getMessage().substring(0, 20) + "..." : request.getMessage();
            ChatSessionEntity sessionEntity = ChatSessionEntity.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .title(title)
                    .build();
            chatSessionRepository.save(sessionEntity);
        }

        // 保存用户消息
        ChatMessageEntity userEntity = ChatMessageEntity.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userEntity);
        return sessionId;
    }

    private void saveAssistantMessage(Long userId, String sessionId, String content) {
        ChatMessageEntity botEntity = ChatMessageEntity.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(ChatMessageRole.ASSISTANT.value())
                .content(content)
                .build();
        chatMessageRepository.save(botEntity);
    }
}
