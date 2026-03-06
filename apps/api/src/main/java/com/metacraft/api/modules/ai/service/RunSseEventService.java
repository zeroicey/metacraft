package com.metacraft.api.modules.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RunSseEventService {

    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sinkMap = new ConcurrentHashMap<>();

    public Flux<ServerSentEvent<String>> subscribe(String runId) {
        Sinks.Many<ServerSentEvent<String>> sink = sinkMap.computeIfAbsent(
                runId,
                key -> Sinks.many().multicast().onBackpressureBuffer()
        );
        return sink.asFlux();
    }

    public void publish(String runId, String event, String data) {
        Sinks.Many<ServerSentEvent<String>> sink = sinkMap.get(runId);
        if (sink == null) {
            return;
        }
        Sinks.EmitResult result = sink.tryEmitNext(ServerSentEvent.<String>builder().event(event).data(data).build());
        if (result.isFailure()) {
            log.debug("Failed to emit SSE event runId={}, event={}, result={}", runId, event, result);
        }
    }

    public void complete(String runId) {
        Sinks.Many<ServerSentEvent<String>> sink = sinkMap.remove(runId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
