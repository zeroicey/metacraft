package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CodeFileAgent {
    @SystemMessage(fromResource = "prompts/gen-code-file.txt")
    @UserMessage("File: {{fileInfoJson}}\n\nContract:\n{{contract}}")
    String generateCodeFile(
        @V("fileInfoJson") String fileInfoJson,
        @V("contract") String contract
    );
}