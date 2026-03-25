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
public class CommentVO {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private OffsetDateTime createdAt;
}