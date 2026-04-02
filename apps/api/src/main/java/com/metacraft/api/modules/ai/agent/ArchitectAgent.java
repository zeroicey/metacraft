package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ArchitectAgent {
    @SystemMessage(fromResource = "prompts/gen-architect.txt")
    @UserMessage("User requirement: {{message}}. App name: {{name}}. App description: {{description}}")
    String generateBlueprint(
        @V("message") String message,
        @V("name") String name,
        @V("description") String description
    );
}