package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ContractAgent {
    @SystemMessage(fromResource = "prompts/gen-contract.txt")
    @UserMessage("Blueprint: {{blueprintJson}}")
    String generateContract(@V("blueprintJson") String blueprintJson);
}