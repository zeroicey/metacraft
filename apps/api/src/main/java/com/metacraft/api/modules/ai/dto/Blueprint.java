package com.metacraft.api.modules.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Blueprint(
    @JsonProperty("project_blueprint") ProjectBlueprint projectBlueprint
) {
    public record ProjectBlueprint(
        @JsonProperty("file_list") List<FileInfo> fileList
    ) {}

    public record FileInfo(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_path") String filePath,
        String purpose,
        @JsonProperty("depends_on") List<String> dependsOn
    ) {}
}