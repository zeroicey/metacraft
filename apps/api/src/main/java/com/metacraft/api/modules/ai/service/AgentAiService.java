package com.metacraft.api.modules.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface AgentAiService {

    @SystemMessage(fromResource = "prompts/agent-system.txt")
    Flux<String> chat(@UserMessage String message);

    @SystemMessage(fromResource = "prompts/agent-gen-app.txt")
    Flux<String> generateApp(@UserMessage String message, @V("userId") Long userId);
}
