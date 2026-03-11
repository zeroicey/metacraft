package com.metacraft.api.modules.ai.service.pipeline;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class AppEditPipelineService {

    public Flux<ServerSentEvent<String>> execute(String message, String history, Long userId, String sessionId) {
        // TODO: 恢复应用修改流水线实现。
        return Flux.empty();
    }
}
