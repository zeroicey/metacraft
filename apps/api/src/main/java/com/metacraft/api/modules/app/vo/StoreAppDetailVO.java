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
public class StoreAppDetailVO {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String logo;
    private Double averageRating;
    private Long ratingCount;
    private AuthorVO author;
    private OffsetDateTime createdAt;
    private List<CommentVO> comments;
}