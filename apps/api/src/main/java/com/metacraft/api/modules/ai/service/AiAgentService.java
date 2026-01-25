package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import ai.z.openapi.service.model.Delta;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.prompt.AgentPrompts;
import com.metacraft.api.modules.ai.repository.ChatMessageRepository;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import com.metacraft.api.modules.ai.vo.PlanResponseVO;
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

    private ZhipuAiClient client;

    @PostConstruct
    public void init() {
        this.client = ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .enableTokenCache()
                .build();
    }

    /**
     * 通用流式处理函数
     */
// 注入 Repository
@Autowired
private ChatMessageRepository chatMessageRepository;

/**
 * 处理流式对话并存储记录
 * * @param userId 用户ID (新增)
 * @param sessionId 会话ID (新增，用于区分不同聊天窗口)
 * @param userContent 用户输入
 * @param systemInstruction 系统指令
 * ...
 */
public SseEmitter processStream(Long userId, String sessionId, String userContent, String systemInstruction, String model, float temperature) {
    SseEmitter emitter = new SseEmitter(0L);

    // 1. 【核心步骤】先存储用户的提问
    // 这一步很重要，因为如果请求失败了，至少用户的记录还在
    ChatMessageEntity userEntry = ChatMessageEntity.builder()
            .userId(userId)
            .sessionId(sessionId)
            .role(ChatMessageRole.USER.value())
            .content(userContent)
            .build();
    chatMessageRepository.save(userEntry);

    // 组合系统提示词
    String fullSystemPrompt = AgentPrompts.SYSTEM + "\n\n" + systemInstruction;

    // 构建 Zhipu 请求消息
    ChatMessage systemMessage = ChatMessage.builder()
            .role(ChatMessageRole.SYSTEM.value())
            .content(fullSystemPrompt)
            .build();

    ChatMessage userMessage = ChatMessage.builder()
            .role(ChatMessageRole.USER.value())
            .content(userContent)
            .build();

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(model)
            .messages(Arrays.asList(systemMessage, userMessage))
            .stream(true)
            .temperature(temperature)
            .maxTokens(4096)
            .build();

    // 2. 准备一个 StringBuilder 用于在流式传输过程中拼接 AI 的完整回复
    StringBuilder aiResponseAccumulator = new StringBuilder();

    ChatCompletionResponse response = client.chat().createChatCompletion(params);

    if (response.isSuccess() && response.getFlowable() != null) {
        response.getFlowable().subscribe(
                data -> {
                    try {
                        if (data.getChoices() != null && !data.getChoices().isEmpty()) {
                            Delta delta = data.getChoices().get(0).getDelta();
                            if (delta != null && delta.getContent() != null) {
                                String content = delta.getContent();
                                if (!content.isEmpty()) {
                                    // A. 推送给前端
                                    emitter.send(SseEmitter.event()
                                            .data(content)
                                            .name("message"));

                                    // B. 【核心步骤】拼接到缓存中
                                    aiResponseAccumulator.append(content);
                                }
                            }
                        }
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    // 发生错误时的处理
                    emitter.completeWithError(error);
                },
                () -> {
                    // 流结束时的处理 (onComplete)
                    try {
                        // 1. 发送结束标志给前端
                        emitter.send(SseEmitter.event().data("[DONE]").name("done"));
                        emitter.complete();

                        // 2. 【核心步骤】流结束了，把拼接好的完整 AI 回复存入数据库
                        String fullAiContent = aiResponseAccumulator.toString();
                        if (!fullAiContent.isEmpty()) {
                            ChatMessageEntity aiEntry = ChatMessageEntity.builder()
                                    .userId(userId)
                                    .sessionId(sessionId)
                                    .role(ChatMessageRole.ASSISTANT.value()) // 标记为 assistant
                                    .content(fullAiContent)
                                    .build();

                            // 注意：这里是在 RxJava 的线程中，Spring Data JPA 是支持的
                            chatMessageRepository.save(aiEntry);
                        }

                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }
        );
    } else {
        emitter.completeWithError(new RuntimeException("请求失败 or 无流式响应"));
    }

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

    public SseEmitter chatStream(AgentRequestDTO request) {
        return processStream(request.getMessage(), AgentPrompts.CHAT, chatModel, 0.7f);
    }

    public PlanResponseVO plan(AgentRequestDTO request) {
        // Plan 目前是非流式，但逻辑也可以复用 prompt 构建
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
                .model(chatModel)
                .messages(Arrays.asList(systemMessage, userMessage))
                .stream(false)
                .temperature(0.5f)
                .maxTokens(2048)
                .build();
                
        ChatCompletionResponse response = client.chat().createChatCompletion(params);
        if (!response.isSuccess()) {
            throw new RuntimeException("规划请求失败: " + response.getMsg());
        }
        String content = response.getData().getChoices().get(0).getMessage().getContent().toString();
        String model = response.getData().getModel();
        return new PlanResponseVO(content, model);
    }

//    public SseEmitter genStream(AgentRequestDTO request) {
//        return processStream(request.getMessage(), AgentPrompts.GEN, genModel, 0.2f);
//    }
}
