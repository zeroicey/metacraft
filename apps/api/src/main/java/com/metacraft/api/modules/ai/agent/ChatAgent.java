package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface ChatAgent {
    @SystemMessage(fromResource = "prompts/chat.txt")
    @UserMessage("""
            Conversation history:
            {{history}}

            Current user message:
            {{message}}
            """)
    Flux<String> chat(@V("message") String message, @V("history") String history);
}
