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
public class StoreAppListVO {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String logo;
    private Double averageRating;
    private Long ratingCount;
    private AuthorVO author;
    private OffsetDateTime createdAt;
}