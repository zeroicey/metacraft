package cc.serenique.api.modules.user.converter;

import cc.serenique.api.modules.user.entity.User;
import cc.serenique.api.modules.user.vo.UserVO;

public final class UserConverter {
    private UserConverter() {}

    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setName(user.getName());
        vo.setBirthday(user.getBirthday());
        vo.setAvatarBase64(user.getAvatarBase64());
        vo.setBio(user.getBio());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
