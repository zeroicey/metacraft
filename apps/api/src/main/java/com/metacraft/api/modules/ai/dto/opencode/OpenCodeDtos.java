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

    /**
     * OpenCode SSE event from /event endpoint
     */
    public record Event(String type, JsonNode properties) {
        /**
         * Get a human-readable description of the event
         */
        public String getDescription() {
            return switch (type) {
                case "content_start" -> "开始生成内容...";
                case "content_delta" -> "生成内容中...";
                case "content_stop" -> "内容生成完成";
                case "tool_use_start" -> {
                    String toolName = properties != null && properties.has("name")
                        ? properties.get("name").asText() : "工具";
                    yield "执行工具: " + toolName;
                }
                case "tool_use_delta" -> "工具执行中...";
                case "tool_use_stop" -> "工具执行完成";
                case "thinking_delta" -> "AI 思考中...";
                case "complete" -> "任务完成";
                case "error" -> {
                    String errorMsg = properties != null && properties.has("message")
                        ? properties.get("message").asText() : "未知错误";
                    yield "错误: " + errorMsg;
                }
                case "warning" -> {
                    String warningMsg = properties != null && properties.has("message")
                        ? properties.get("message").asText() : "警告";
                    yield "警告: " + warningMsg;
                }
                default -> "处理中: " + type;
            };
        }

        /**
         * Check if this event indicates the task is still in progress
         */
        public boolean isInProgress() {
            return switch (type) {
                case "content_start", "content_delta", "tool_use_start",
                     "tool_use_delta", "thinking_delta" -> true;
                default -> false;
            };
        }

        /**
         * Check if this event indicates the task is complete
         */
        public boolean isComplete() {
            return "complete".equals(type);
        }

        /**
         * Check if this event indicates an error
         */
        public boolean isError() {
            return "error".equals(type);
        }
    }
}