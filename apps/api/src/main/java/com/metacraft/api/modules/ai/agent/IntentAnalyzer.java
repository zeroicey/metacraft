package com.metacraft.api.modules.ai.agent;

import com.metacraft.api.modules.ai.dto.IntentDTO;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface IntentAnalyzer {
    @SystemMessage(fromResource = "prompts/intent.txt")
    @UserMessage("User request: {{it}}")
    IntentDTO analyze(String message);
}