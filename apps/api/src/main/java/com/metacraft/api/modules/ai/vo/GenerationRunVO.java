package com.metacraft.api.modules.ai.vo;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class GenerationRunVO {
    private String runId;
    private String sessionId;
    private String intent;
    private String status;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime completedAt;
}
