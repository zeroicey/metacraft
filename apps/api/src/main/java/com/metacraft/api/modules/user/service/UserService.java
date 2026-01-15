package com.metacraft.api.modules.user.service;

import com.metacraft.api.modules.user.converter.UserConverter;
import com.metacraft.api.modules.user.dto.UserLoginDTO;
import com.metacraft.api.modules.user.dto.UserRegisterDTO;
import com.metacraft.api.modules.user.dto.UserUpdateDTO;
import com.metacraft.api.modules.user.entity.User;
import com.metacraft.api.modules.user.repository.UserRepository;
import com.metacraft.api.modules.user.vo.AuthTokenVO;
import com.metacraft.api.modules.user.vo.UserVO;
import com.metacraft.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthTokenVO register(UserRegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("用户已存在");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setAvatarBase64(dto.getAvatarBase64() != null ? dto.getAvatarBase64() : "");
        user.setBio(dto.getBio() != null ? dto.getBio() : "");

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new AuthTokenVO(token, jwtExpiration / 1000);
    }

    public AuthTokenVO login(UserLoginDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return new AuthTokenVO(token, jwtExpiration / 1000);
    }

    public UserVO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return UserConverter.toVO(user);
    }

    @Transactional
    public UserVO updateUser(String email, UserUpdateDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }

        if (dto.getAvatarBase64() != null) {
            user.setAvatarBase64(dto.getAvatarBase64());
        }

        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }

        if (dto.getCurrentPassword() != null && dto.getNewPassword() != null) {
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
                throw new RuntimeException("当前密码错误");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        } else if (dto.getCurrentPassword() != null || dto.getNewPassword() != null) {
            throw new RuntimeException("修改密码需要同时提供当前密码和新密码");
        }

        User updatedUser = userRepository.save(user);
        return UserConverter.toVO(updatedUser);
    }
}
