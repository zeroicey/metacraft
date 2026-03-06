package com.metacraft.api.modules.ai.vo;

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

    // App binding info (when type = "app")
    private String type;
    private Long relatedAppId;
    private Long relatedVersionId;
    private String relatedAppUuid;
    private String relatedAppName;
    private String relatedAppDescription;
    private String relatedAppLogo;
}
