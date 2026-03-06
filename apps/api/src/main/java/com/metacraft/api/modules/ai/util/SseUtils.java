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

    public String toRunStartedJson(String runId, String sessionId) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("sessionId", sessionId != null ? sessionId : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize run_started data", e);
            return "{\"runId\":\"\",\"sessionId\":\"\"}";
        }
    }

    public String toIntentJson(String runId, String intent) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("intent", intent != null ? intent : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize run intent data", e);
            return "{\"runId\":\"\",\"intent\":\"\"}";
        }
    }

    public String toContentJson(String runId, String content) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("content", content != null ? content : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize run content data", e);
            return "{\"runId\":\"\",\"content\":\"\"}";
        }
    }

    public String toDoneJson(String runId) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize run done data", e);
            return "{\"runId\":\"\"}";
        }
    }

    public String toErrorJson(String runId, String message) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("message", message != null ? message : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize run error data", e);
            return "{\"runId\":\"\",\"message\":\"\"}";
        }
    }

    public String toAppSavedJson(String runId, Long appId, String appUuid, Long versionId, String previewUrl, String name, String description, String logoUrl) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("appId", appId);
            data.put("appUuid", appUuid != null ? appUuid : "");
            data.put("versionId", versionId);
            data.put("previewUrl", previewUrl != null ? previewUrl : "");
            data.put("name", name != null ? name : "");
            data.put("description", description != null ? description : "");
            data.put("logoUrl", logoUrl != null ? logoUrl : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize app_saved data", e);
            return "{\"runId\":\"\",\"previewUrl\":\"\"}";
        }
    }

    public String toSpecReadyJson(String runId, String name, String description) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("name", name != null ? name : "");
            data.put("description", description != null ? description : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize spec_ready data", e);
            return "{\"runId\":\"\",\"name\":\"\",\"description\":\"\"}";
        }
    }

    public String toRunOnlyJson(String runId) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize run-only data", e);
            return "{\"runId\":\"\"}";
        }
    }

    public String toLogoStartedJson(String runId, String logoUuid) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("logoUuid", logoUuid != null ? logoUuid : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize logo_started data", e);
            return "{\"runId\":\"\",\"logoUuid\":\"\"}";
        }
    }

    public String toLogoReadyJson(String runId, String logoUrl) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("logoUrl", logoUrl != null ? logoUrl : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize logo_ready data", e);
            return "{\"runId\":\"\",\"logoUrl\":\"\"}";
        }
    }

    public String toLogoFailedJson(String runId, String reason) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("runId", runId != null ? runId : "");
            data.put("reason", reason != null ? reason : "");
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize logo_failed data", e);
            return "{\"runId\":\"\",\"reason\":\"\"}";
        }
    }

}
