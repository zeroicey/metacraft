package com.metacraft.api.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSessionUpdateDTO {
    @NotBlank(message = "Title cannot be empty")
    private String title;
}
