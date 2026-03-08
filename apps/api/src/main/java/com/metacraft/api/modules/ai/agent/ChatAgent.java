package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface ChatAgent {
    @SystemMessage(fromResource = "prompts/chat.txt")
    Flux<String> chat(String message);

    @SystemMessage(fromResource = "prompts/gen-chat.txt")
    Flux<String> chatBeforeGen(String message);
}
