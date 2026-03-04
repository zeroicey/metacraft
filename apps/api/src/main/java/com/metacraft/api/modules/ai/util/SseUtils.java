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
     * Safely serialize content to JSON string using Jackson.
     * Handles all special characters, Unicode, and escaping automatically.
     *
     * @param content The content to serialize
     * @return JSON string representation (e.g., "\"content\"")
     */
    public String toJson(String content) {
        if (content == null) {
            return "\"\"";
        }
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception e) {
            log.error("JSON serialization failed for content: {}", content.substring(0, Math.min(100, content.length())), e);
            return "\"\""; // Fallback to empty JSON string
        }
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
