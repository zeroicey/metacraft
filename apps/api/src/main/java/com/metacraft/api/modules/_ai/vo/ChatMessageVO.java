package com.metacraft.api.modules._ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private OffsetDateTime createdAt;
}
