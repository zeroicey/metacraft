package com.metacraft.api.modules.ai.service.pipeline;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import com.metacraft.api.modules.ai.agent.ChatAgent;
import com.metacraft.api.modules.ai.util.SseUtils;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AppEditPipelineService {

    private final ChatAgent chatAgent;
    private final SseUtils sseUtils;

    public Flux<ServerSentEvent<String>> execute(String message) {
        return chatAgent.chat(message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(sseUtils.toContentJson(chunk))
                        .build());
    }
}
