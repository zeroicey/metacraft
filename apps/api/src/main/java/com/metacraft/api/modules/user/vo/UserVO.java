package cc.serenique.api.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class UserVO {
    private Short id;
    private String email;
    private String name;
    private LocalDate birthday;
    private String avatarBase64;
    private String bio;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
