package com.metacraft.api.modules.ai.agent;

import reactor.core.publisher.Flux;

public interface CodeGenerator {
    Flux<String> generateCode(String message, Long userId, String logoUuid, String runId);
}