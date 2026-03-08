package com.metacraft.api.modules.ai.agent;

import com.metacraft.api.modules.ai.dto.AppCodeDTO;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CodeGenerator {
    @SystemMessage(fromResource = "prompts/gen-code.txt")
    @UserMessage("Build an app with the following user requirement: {{requirement}}. App Name: {{name}}. App Description: {{description}}")
    AppCodeDTO generateCode(@V("requirement") String requirement, @V("name") String name,
            @V("description") String description);
}