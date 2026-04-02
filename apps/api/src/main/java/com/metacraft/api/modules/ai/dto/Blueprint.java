package com.metacraft.api.modules.ai.dto;

import java.util.List;

public record Blueprint(
    ProjectBlueprint projectBlueprint
) {
    public record ProjectBlueprint(
        List<FileInfo> fileList
    ) {}

    public record FileInfo(
        String fileId,
        String filePath,
        String purpose,
        List<String> dependsOn
    ) {}
}