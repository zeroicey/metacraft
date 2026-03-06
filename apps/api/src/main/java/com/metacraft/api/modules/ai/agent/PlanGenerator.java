package com.metacraft.api.modules.ai.agent;

import reactor.core.publisher.Flux;

public interface PlanGenerator {
    Flux<String> generatePlan(String message, Long userId, String logoUuid, String runId);
}