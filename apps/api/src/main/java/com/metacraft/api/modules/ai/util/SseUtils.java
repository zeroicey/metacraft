package com.metacraft.api.modules.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
        } catch (Exception e) {
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
        } catch (Exception e) {
            log.error("Failed to serialize content data", e);
            return "{\"content\":\"\"}";
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
     * Create app_generated event data as a JSON object.
     * Format: {"url":"...", "uuid":"...", "name":"...", "description":"..."}
     *
     * @param url The preview URL
     * @param uuid The app UUID
     * @param name The app name
     * @param description The app description
     * @return JSON object string representation
     */
    public String toAppGeneratedJson(String url, String uuid, String name, String description) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("url", url != null ? url : "");
            data.put("uuid", uuid != null ? uuid : "");
            data.put("name", name != null ? name : "");
            data.put("description", description != null ? description : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize app_generated data", e);
            // Fallback to minimal JSON object
            return "{\"url\":\"\",\"uuid\":\"\",\"name\":\"\",\"description\":\"\"}";
        }
    }
}
