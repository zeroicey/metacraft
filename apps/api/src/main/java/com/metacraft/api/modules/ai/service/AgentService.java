package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentIntentService intentService;
    private final AgentAiService agentAiService;

    public Flux<ServerSentEvent<String>> unified(AgentRequestDTO request, Long userId) {
        return Mono.fromCallable(() -> {
            // 1. Identify Intent
            AgentIntentRequestDTO intentReq = new AgentIntentRequestDTO();
            intentReq.setMessage(request.getMessage());
            return intentService.classifyIntent(intentReq);
        })
        .subscribeOn(Schedulers.boundedElastic()) // Run intent classification on blocking thread
        .flatMapMany(intent -> {
            log.info("User {} intent: {}", userId, intent);
            
            // Send intent event first
            ServerSentEvent<String> intentEvent = ServerSentEvent.<String>builder()
                    .event("intent")
                    .data(intent)
                    .build();

            Flux<String> aiStream;
            
            if ("gen".equalsIgnoreCase(intent)) {
                // Pass userId to AI service for tool execution
                aiStream = agentAiService.generateApp(request.getMessage(), userId);
            } else {
                aiStream = agentAiService.chat(request.getMessage());
            }

            // Map AI stream to SSE
            Flux<ServerSentEvent<String>> messageStream = aiStream
                    .map(content -> ServerSentEvent.<String>builder()
                            .event("message")
                            .data(content)
                            .build());
                    
            return Flux.concat(Flux.just(intentEvent), messageStream);
        });
    }
}
