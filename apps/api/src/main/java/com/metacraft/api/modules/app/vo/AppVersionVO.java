package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersionVO {
    private Long id;
    private Integer versionNumber;
    private String storagePath;
    private String changeLog;
    private OffsetDateTime createdAt;
}
