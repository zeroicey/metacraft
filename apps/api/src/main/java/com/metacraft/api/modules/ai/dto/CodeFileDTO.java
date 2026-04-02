package com.metacraft.api.modules.ai.dto;

public record CodeFileDTO(
    String fileId,
    String filePath,
    String code
) {}