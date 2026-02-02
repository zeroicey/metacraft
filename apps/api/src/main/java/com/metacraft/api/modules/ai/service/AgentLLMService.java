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
                                            // 生产级方案：使用 JSON 包装内容，自动处理所有特殊字符（空格、换行等）
                                            // 这避免了自定义协议 (<SPACE>) 的潜在冲突，是行业标准做法。
                                            java.util.Map<String, String> chunk = new java.util.HashMap<>();
                                            chunk.put("content", content);
                                            
                                            emitter.send(SseEmitter.event()
                                                    .data(chunk)
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
                            log.error("Stream error", error);
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
    
    /**
     * 同步调用 (用于意图识别等)
     */
    public ChatCompletionResponse executeSync(ChatCompletionCreateParams params) {
        return client.chat().createChatCompletion(params);
    }
}
