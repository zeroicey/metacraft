package cc.serenique.api.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserUpdateDTO {
    @Email(message = "email must be a valid email address")
    private String email;

    private String name;

    @Past(message = "birthday must be in the past")
    private LocalDate birthday;

    private String avatarBase64;

    private String bio;

    private String currentPassword;

    private String newPassword;
}
