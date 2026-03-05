package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVO {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private Boolean isPublic;
    private Long currentVersionId;
    private List<AppVersionVO> versions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
