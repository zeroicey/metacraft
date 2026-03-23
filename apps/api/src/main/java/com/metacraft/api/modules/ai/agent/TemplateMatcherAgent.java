package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface TemplateMatcherAgent {
    @SystemMessage(fromResource = "prompts/template-match.txt")
    @UserMessage("templates: {{templates}}\nuser request: {{message}}")
    Flux<String> matchTemplates(@V("templates") String templates, @V("message") String message);
}