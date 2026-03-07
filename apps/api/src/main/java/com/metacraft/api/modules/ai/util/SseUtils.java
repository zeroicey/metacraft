package com.metacraft.api.modules.ai.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.ai.entity.AppInfoEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for SSE JSON serialization using Jackson.
 * Provides reliable JSON encoding without manual string escaping issues.
 */
@Slf4j
@Component
public class SseUtils {

    private final ObjectMapper objectMapper;

    public SseUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create intent event data as a JSON object.
     * Format: {"intent":"chat" or "gen"}
     */
    public String toIntentJson(String intent) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("intent", intent != null ? intent : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize intent data", e);
            return "{\"intent\":\"\"}";
        }
    }

    /**
     * Create content event data as a JSON object for message/plan events.
     * Format: {"content":"..."}
     */
    public String toContentJson(String content) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("content", content != null ? content : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize content data", e);
            return "{\"content\":\"\"}";
        }
    }

    /**
     * Create plan event data as a JSON object.
     * Format: {"plan":"..."}
     */
    public String toPlanJson(String plan) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("plan", plan != null ? plan : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize plan data", e);
            return "{\"plan\":\"\"}";
        }
    }

    /**
     * Create done event data as a JSON object.
     * Format: {}
     */
    public String toDoneJson() {
        return "{}";
    }

    /**
     * Create error event data as a JSON object.
     * Format: {"message":"..."}
     */
    public String toErrorJson(String message) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("error", message != null ? message : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error data", e);
            return "{\"error\":\"\"}";
        }
    }

    /**
     * Create app_generated event data as a JSON object.
     * Format: {"url":"...", "uuid":"...", "name":"...", "description":"...",
     * "logo":"..."}
     *
     * @param url         The preview URL
     * @param uuid        The app UUID
     * @param name        The app name
     * @param description The app description
     * @param logo        The app logo URL (e.g. /api/logo/{uuid})
     * @return JSON object string representation
     */
    public String toAppGeneratedJson(String url, String uuid, String name, String description, String logo) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("url", url != null ? url : "");
            data.put("uuid", uuid != null ? uuid : "");
            data.put("name", name != null ? name : "");
            data.put("description", description != null ? description : "");
            data.put("logo", logo != null ? logo : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize app_generated data", e);
            // Fallback to minimal JSON object
            return "{\"url\":\"\",\"uuid\":\"\",\"name\":\"\",\"description\":\"\",\"logo\":\"\"}";
        }
    }

    /**
     * Create appinfo event data as a JSON object.
     * Format: {"name":"...", "description":"..."}
     */
    public String toAppInfoJson(AppInfoEntity appInfo) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("name", appInfo != null && appInfo.getName() != null ? appInfo.getName() : "");
            data.put("description", appInfo != null && appInfo.getDescription() != null ? appInfo.getDescription() : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize appinfo data", e);
            return "{\"name\":\"\",\"description\":\"\"}";
        }
    }

    /**
     * Create logo_generated event data as a JSON object.
     * Format: {"uuid":"...", "ext":"..."}
     */
    public String toLogoGerneratedJson(String uuid, String ext) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("uuid", uuid != null ? uuid : "");
            data.put("ext", ext != null ? ext : "");
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize logo_generated data", e);
            return "{\"uuid\":\"\",\"ext\":\"\"}";
        }
    }
}
