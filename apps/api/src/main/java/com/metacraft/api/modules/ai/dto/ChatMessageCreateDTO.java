package com.metacraft.api.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageCreateDTO {
    @NotBlank(message = "Session ID cannot be empty")
    private String sessionId;

    @NotBlank(message = "Role cannot be empty")
    private String role;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String type = "text"; // "text" or "app"

    private Long relatedAppId;

    private Long relatedVersionId;
}
