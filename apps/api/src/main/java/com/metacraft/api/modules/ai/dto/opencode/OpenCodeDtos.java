package com.metacraft.api.modules.ai.dto.opencode;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public final class OpenCodeDtos {

    private OpenCodeDtos() {
    }

    public record HealthResponse(boolean healthy, String version) {
    }

    public record Session(String id) {
    }

    public record CreateSessionRequest(String title) {
    }

    public record TextPart(String type, String text) {
        public static TextPart text(String text) {
            return new TextPart("text", text);
        }
    }

    public record MessageRequest(List<TextPart> parts) {
        public static MessageRequest text(String prompt) {
            return new MessageRequest(List.of(TextPart.text(prompt)));
        }
    }

    public record MessageEnvelope(JsonNode info, List<JsonNode> parts) {
        public String textContent() {
            if (parts == null || parts.isEmpty()) {
                return "";
            }

            StringBuilder builder = new StringBuilder();
            for (JsonNode part : parts) {
                if (part == null) {
                    continue;
                }

                JsonNode typeNode = part.get("type");
                JsonNode textNode = part.get("text");
                if (typeNode != null && "text".equals(typeNode.asText()) && textNode != null) {
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
    }
}