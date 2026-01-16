package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import com.metacraft.api.modules.ai.dto.ChatRequestDTO;
import com.metacraft.api.modules.ai.vo.ChatResponseVO;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;

@Service
public class AiChatService {

    @Value("${zhipuai.api-key}")
    private String apiKey;

    @Value("${zhipuai.chat.model:glm-4}")
    private String chatModel;

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
     * 发送聊天消息(非流式)
     */
    public ChatResponseVO chat(ChatRequestDTO request) {
        // 创建聊天消息
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        // 创建聊天请求
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(chatModel)
                .messages(Collections.singletonList(userMessage))
                .stream(false)
                .temperature(0.95f)
                .maxTokens(1024)
                .build();

        // 调用智谱 AI 聊天接口
        ChatCompletionResponse response = client.chat().createChatCompletion(params);

        if (!response.isSuccess()) {
            throw new RuntimeException("聊天请求失败: " + response.getMsg());
        }

        // 提取 AI 回复内容
        String reply = response.getData().getChoices().get(0).getMessage().getContent().toString();
        String model = response.getData().getModel();

        return new ChatResponseVO(reply, model);
    }

    /**
     * 发送聊天消息(流式)
     */
    public SseEmitter chatStream(ChatRequestDTO request) {
        SseEmitter emitter = new SseEmitter(0L);

        // 创建聊天消息
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(request.getMessage())
                .build();

        // 创建流式聊天请求
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(chatModel)
                .messages(Collections.singletonList(userMessage))
                .stream(true)
                .temperature(0.95f)
                .maxTokens(1024)
                .build();

        // 调用智谱 AI 流式聊天接口
        ChatCompletionResponse response = client.chat().createChatCompletion(params);

        // 处理流式响应
        if (response.isSuccess() && response.getFlowable() != null) {
            response.getFlowable().subscribe(
                    data -> {
                        try {
                            // 处理流式数据块
                            if (data.getChoices() != null && !data.getChoices().isEmpty()) {
                                Delta delta = data.getChoices().get(0).getDelta();
                                if (delta != null && delta.getContent() != null) {
                                    String content = delta.getContent();
                                    if (!content.isEmpty()) {
                                        emitter.send(SseEmitter.event()
                                                .data(content)
                                                .name("message"));
                                    }
                                }
                            }
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    emitter::completeWithError,
                    () -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .data("[DONE]")
                                    .name("done"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
            );
        } else {
            emitter.completeWithError(new RuntimeException("聊天请求失败或无法获取流式响应"));
        }

        return emitter;
    }
}
