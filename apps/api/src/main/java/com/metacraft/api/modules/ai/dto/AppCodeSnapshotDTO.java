package com.metacraft.api.modules.ai.dto;

public record AppCodeSnapshotDTO(
    String htmlCode,
    String jsCode,
    Long versionId,
    Integer version
) {}