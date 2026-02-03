package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.Delta;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
@Service
public class AgentLLMService {

    @Value("${zhipuai.api-key}")
    private String apiKey;

    private ZhipuAiClient client;

    @PostConstruct
    public void init() {
        this.client = ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .enableTokenCache()
                .build();
    }

    public ZhipuAiClient getClient() {
        return client;
    }

    /**
     * 通用流式执行器
     *
     * @param params          Zhipu 请求参数
     * @param onRealTimeChunk 实时回调(可选)
     * @param onComplete      完成回调(可选)
     */
    public void executeStream(
            SseEmitter emitter,
            ChatCompletionCreateParams params,
            Consumer<String> onRealTimeChunk,
            Consumer<String> onComplete
    ) {
        executeStreamWithCallback(params, 
            chunk -> {
                try {
                    // 1. 发送给前端
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    data.put("content", chunk);
                    
                    emitter.send(SseEmitter.event()
                            .data(data)
                            .name("message"));

                    // 2. 实时回调
                    if (onRealTimeChunk != null) {
                        onRealTimeChunk.accept(chunk);
                    }
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            fullContent -> {
                try {
                    emitter.send(SseEmitter.event().data("[DONE]").name("done"));
                    emitter.complete();

                    // 3. 完成回调
                    if (onComplete != null) {
                        onComplete.accept(fullContent);
                    }
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            },
            error -> {
                log.error("Stream error", error);
                emitter.completeWithError(error);
            }
        );
    }

    /**
     * 自定义回调流式执行器 (不依赖 SseEmitter)
     *
     * @param params          Zhipu 请求参数
     * @param onChunk         实时 Chunk 回调
     * @param onComplete      完成回调
     * @param onError         错误回调
     */
    public void executeStreamWithCallback(
            ChatCompletionCreateParams params,
            Consumer<String> onChunk,
            Consumer<String> onComplete,
            Consumer<Throwable> onError
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
                                        accumulator.append(content);
                                        if (onChunk != null) {
                                            onChunk.accept(content);
                                        }
                                    }
                                }
                            }
                        },
                        error -> {
                            if (onError != null) {
                                onError.accept(error);
                            }
                        },
                        () -> {
                            if (onComplete != null) {
                                onComplete.accept(accumulator.toString());
                            }
                        }
                );
    }
    
    /**
     * 同步调用 (用于意图识别等)
     */
    public ChatCompletionResponse executeSync(ChatCompletionCreateParams params) {
        return client.chat().createChatCompletion(params);
    }
}
