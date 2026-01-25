package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.prompt.AgentPrompts;
import com.metacraft.api.modules.ai.repository.ChatMessageRepository;
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

    public AgentService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
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
    private SseEmitter executeStream(
            ChatCompletionCreateParams params,
            java.util.function.Consumer<String> onRealTimeChunk,
            java.util.function.Consumer<String> onComplete
    ) {
        SseEmitter emitter = new SseEmitter(0L);
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

        return emitter;
    }

    public SseEmitter chatStream(AgentRequestDTO request) {
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

        return executeStream(params, null, null);
    }

    public SseEmitter genStream(AgentRequestDTO request) {
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

        return executeStream(params, null, null);
    }

    public SseEmitter planStream(AgentRequestDTO request) {
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

        return executeStream(params, null, null);
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
}
