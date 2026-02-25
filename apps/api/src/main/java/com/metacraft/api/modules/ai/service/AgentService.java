package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.service.AppService;
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
    private final AppService appService;

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

            String eventType = "gen".equalsIgnoreCase(intent) ? "plan" : "message";

            // Stream AI content as plan/message events
            Flux<ServerSentEvent<String>> messageStream = aiStream
                    .map(content -> ServerSentEvent.<String>builder()
                            .event(eventType)
                            .data(content)
                            .build());

            // After stream ends, check if user has a new app and send app_generated event
            Flux<ServerSentEvent<String>> postStream = Flux.defer(() -> {
                if ("gen".equalsIgnoreCase(intent)) {
                    // Get the latest app for this user
                    AppEntity latestApp = appService.getLatestAppByUserId(userId);
                    if (latestApp != null) {
                        String previewUrl = "/api/preview/" + latestApp.getUuid() + "/v/1";
                        log.info("Gen mode completed, sending app_generated event with URL: {}", previewUrl);
                        return Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("app_generated")
                                        .data(previewUrl)
                                        .build()
                        );
                    }
                }
                return Flux.empty();
            });

            // Send done event at the end
            Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("done")
                            .data("")
                            .build()
            );

            return Flux.concat(Flux.just(intentEvent), messageStream, postStream, doneEvent);
        });
    }
}
