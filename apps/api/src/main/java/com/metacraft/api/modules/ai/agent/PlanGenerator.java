package com.metacraft.api.modules.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface PlanGenerator {
    @SystemMessage(fromResource = "prompts/gen-plan.txt")
    @UserMessage("User requirement: {{it}}")
    Flux<String> generatePlan(String message);
}