package com.metacraft.api.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class UserVO {
    private Long id;
    private String email;
    private String name;
    private String avatarBase64;
    private String bio;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
