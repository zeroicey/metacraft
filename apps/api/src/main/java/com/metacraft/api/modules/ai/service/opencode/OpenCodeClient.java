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

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.ai.config.OpenCodeProperties;
import com.metacraft.api.modules.ai.dto.opencode.OpenCodeDtos;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpenCodeClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final ObjectMapper objectMapper;
    private final OpenCodeProperties properties;
    private final HttpClient httpClient;

    public OpenCodeClient(ObjectMapper objectMapper, OpenCodeProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
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
        return createSession(new OpenCodeDtos.CreateSessionRequest(title, null));
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

    public OpenCodeDtos.MessageEnvelope sendMessage(String sessionId, OpenCodeDtos.MessageRequest request) {
        return sendRequest("POST", "/session/" + encodePath(sessionId) + "/message", request,
                OpenCodeDtos.MessageEnvelope.class);
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