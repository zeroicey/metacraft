package com.metacraft.api.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthTokenVO {
    private String token;
    private String type = "Bearer";
    private Long expiresIn;

    public AuthTokenVO(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
