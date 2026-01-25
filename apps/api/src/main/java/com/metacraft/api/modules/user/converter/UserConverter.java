package com.metacraft.api.modules.user.converter;


import com.metacraft.api.modules.user.entity.UserEntity;
import com.metacraft.api.modules.user.vo.UserVO;

public final class UserConverter {
    private UserConverter() {}

    public static UserVO toVO(UserEntity user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setName(user.getName());
        vo.setAvatarBase64(user.getAvatarBase64());
        vo.setBio(user.getBio());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
