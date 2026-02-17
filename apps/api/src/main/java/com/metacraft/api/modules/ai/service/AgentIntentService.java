package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentIntentService {

    @Resource
    private ChatModel chatModel;

    private static final String INTENT_PROMPT_TEMPLATE = """
        Analyze the user's input and classify their intent into exactly one of the following two categories:
        1. 'chat': The user wants to chat, ask questions, or discuss general topics.
        2. 'gen': The user wants to generate a webpage, an app, a tool, or code.

        User Input: {{message}}

        Output ONLY the category name ('chat' or 'gen'). Do not include any punctuation, explanation, or extra text.
    """;

    public String classifyIntent(AgentIntentRequestDTO request) {
        PromptTemplate promptTemplate = PromptTemplate.from(INTENT_PROMPT_TEMPLATE);
        Prompt prompt = promptTemplate.apply(Map.of("message", request.getMessage()));
        String response = chatModel.chat(prompt.text());
        
        String intent = response.trim().toLowerCase();
        if (intent.contains("gen")) {
            intent = "gen";
        } else {
            intent = "chat";
        }
        
        return intent;
    }
}
