package com.metacraft.api.modules.ai.service.opencode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.ai.config.OpenCodeProperties;
import com.metacraft.api.modules.ai.dto.opencode.OpenCodeDtos;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class OpenCodeClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(30);

    private final ObjectMapper objectMapper;
    private final OpenCodeProperties properties;
    private final HttpClient httpClient;
    private final RestClient restClient;

    public OpenCodeClient(ObjectMapper objectMapper, OpenCodeProperties properties,
                          RestTemplateBuilder restTemplateBuilder) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .build();
    }

    public OpenCodeDtos.HealthResponse health() {
        return sendRequest("GET", "/global/health", null, OpenCodeDtos.HealthResponse.class);
    }

    public List<OpenCodeDtos.Session> listSessions() {
        return sendRequest("GET", "/session", null, new TypeReference<List<OpenCodeDtos.Session>>() {
        });
    }

    public OpenCodeDtos.Session createSession(String title) {
        return createSession(new OpenCodeDtos.CreateSessionRequest(title));
    }

    public OpenCodeDtos.Session createSession(OpenCodeDtos.CreateSessionRequest request) {
        return sendRequest("POST", "/session", request, OpenCodeDtos.Session.class);
    }

    public boolean deleteSession(String sessionId) {
        return sendRequest("DELETE", "/session/" + encodePath(sessionId), null, Boolean.class);
    }

    public List<OpenCodeDtos.MessageEnvelope> listMessages(String sessionId, Integer limit) {
        String path = "/session/" + encodePath(sessionId) + "/message";
        if (limit != null) {
            path = path + "?limit=" + limit;
        }
        return sendRequest("GET", path, null, new TypeReference<List<OpenCodeDtos.MessageEnvelope>>() {
        });
    }

    public boolean sendMessage(String sessionId, OpenCodeDtos.MessageRequest request) {
        HttpRequest httpRequest = buildRequest("POST", "/session/" + encodePath(sessionId) + "/message", request);

        try {
            HttpResponse<Void> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenCodeClientException(
                        "OpenCode request failed: POST /session/" + encodePath(sessionId) + "/message -> "
                                + response.statusCode());
            }
            return true;
        } catch (IOException exception) {
            throw new OpenCodeClientException("OpenCode request I/O failure: POST /session/" + encodePath(sessionId)
                    + "/message", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenCodeClientException(
                    "OpenCode request interrupted: POST /session/" + encodePath(sessionId) + "/message",
                    exception);
        }
    }

    /**
     * Subscribe to SSE events from OpenCode server.
     * This provides real-time updates on AI progress (tool execution, content generation, etc.)
     *
     * @return Flux of OpenCode events
     */
    public Flux<OpenCodeDtos.Event> subscribeEvents() {
        return Flux.<OpenCodeDtos.Event>create(emitter -> {
            Thread.startVirtualThread(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(properties.getBaseUrl() + "/event"))
                            .timeout(REQUEST_TIMEOUT)
                            .header("Accept", "text/event-stream")
                            .header("Authorization", basicAuthHeader())
                            .GET()
                            .build();

                    HttpClient client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();

                    client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                            .thenAccept(response -> {
                                if (response.statusCode() != 200) {
                                    emitter.error(new OpenCodeClientException(
                                            "Failed to subscribe to events: " + response.statusCode()));
                                    return;
                                }

                                try {
                                    response.body().forEach(line -> {
                                        if (emitter.isCancelled()) {
                                            return;
                                        }

                                        if (line != null && !line.isBlank() && line.startsWith("data:")) {
                                            try {
                                                String json = line.substring(5).trim();
                                                JsonNode node = objectMapper.readTree(json);
                                                String type = node.has("type") ? node.get("type").asText() : "unknown";
                                                JsonNode props = node.has("properties") ? node.get("properties") : null;
                                                OpenCodeDtos.Event event = new OpenCodeDtos.Event(type, props);
                                                emitter.next(event);
                                            } catch (JsonProcessingException e) {
                                                log.warn("Failed to parse SSE event: {}", line);
                                            }
                                        }
                                    });
                                    emitter.complete();
                                } catch (Exception e) {
                                    emitter.error(e);
                                }
                            });
                } catch (Exception e) {
                    emitter.error(e);
                }
            });
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .doOnNext(event -> log.debug("OpenCode event: {}", event.type()))
                .doOnComplete(() -> log.info("Event stream completed"))
                .doOnError(e -> log.error("Event stream error", e));
    }

    /**
     * Subscribe to SSE events and filter for specific session.
     * Note: OpenCode /event endpoint returns global events. For session-specific events,
     * you may need to filter by session ID in the event properties.
     *
     * @param sessionId Optional session ID to filter events (can be null for all events)
     * @return Flux of filtered OpenCode events
     */
    public Flux<OpenCodeDtos.Event> subscribeEventsForSession(String sessionId) {
        return subscribeEvents()
                .filter(event -> {
                    if (sessionId == null || sessionId.isBlank()) {
                        return true;
                    }
                    // Filter events that belong to the specified session
                    if (event.properties() != null && event.properties().has("sessionId")) {
                        return sessionId.equals(event.properties().get("sessionId").asText());
                    }
                    // Include events without session ID (global events)
                    return true;
                });
    }

    private <T> T sendRequest(String method, String path, Object body, Class<T> responseType) {
        String responseBody = execute(method, path, body);
        if (responseType == Boolean.class) {
            return responseType.cast(Boolean.valueOf(responseBody));
        }
        return readValue(responseBody, responseType);
    }

    private <T> T sendRequest(String method, String path, Object body, TypeReference<T> responseType) {
        String responseBody = execute(method, path, body);
        return readValue(responseBody, responseType);
    }

    private String execute(String method, String path, Object body) {
        HttpRequest request = buildRequest(method, path, body);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OpenCodeClientException(
                        "OpenCode request failed: " + method + " " + path + " -> " + response.statusCode()
                                + ", body=" + response.body());
            }
            return response.body();
        } catch (IOException exception) {
            throw new OpenCodeClientException("OpenCode request I/O failure: " + method + " " + path, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenCodeClientException("OpenCode request interrupted: " + method + " " + path, exception);
        }
    }

    private HttpRequest buildRequest(String method, String path, Object body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/json");

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
            return builder.build();
        }

        builder.method(method, HttpRequest.BodyPublishers.ofString(writeValue(body)));
        return builder.build();
    }

    private String writeValue(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new OpenCodeClientException("Failed to serialize OpenCode request body", exception);
        }
    }

    private <T> T readValue(String body, Class<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JsonProcessingException exception) {
            throw new OpenCodeClientException("Failed to deserialize OpenCode response", exception);
        }
    }

    private <T> T readValue(String body, TypeReference<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JsonProcessingException exception) {
            throw new OpenCodeClientException("Failed to deserialize OpenCode response", exception);
        }
    }

    private String basicAuthHeader() {
        String credentials = properties.getUsername() + ":" + properties.getPassword();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}