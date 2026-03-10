package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SessionTitleGenerator {
    @SystemMessage(fromResource = "prompts/gen-session-title.txt")
    @UserMessage("User message: {{it}}")
    String generateTitle(String message);
}
