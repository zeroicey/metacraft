package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CodeGenerator {
    @SystemMessage(fromResource = "prompts/gen-code.txt")
    @UserMessage("User requirement: {{it}}")
    String generateCode(String message);
}