package com.metacraft.api.modules.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    @Email(message = "email must be a valid email address")
    private String email;

    private String name;

    private String avatarBase64;

    private String bio;

    private String currentPassword;

    private String newPassword;
}
