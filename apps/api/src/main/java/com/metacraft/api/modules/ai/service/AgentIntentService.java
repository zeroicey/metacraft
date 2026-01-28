package com.metacraft.api.modules.ai.service;

import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.entity.ChatMessage;
import com.metacraft.api.modules.ai.entity.ChatMessageRole;
import com.metacraft.api.modules.ai.prompt.AgentPrompts;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentIntentService {

    private final AgentLLMService llmService;

    @Value("${zhipuai.intent.model:glm-4}")
    private String intentModel;

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

        ChatCompletionResponse response = llmService.executeSync(params);
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
