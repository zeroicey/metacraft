package com.metacraft.api.modules.user.controller;


import com.metacraft.api.modules.user.dto.UserLoginDTO;
import com.metacraft.api.modules.user.dto.UserRegisterDTO;
import com.metacraft.api.modules.user.service.UserService;
import com.metacraft.api.modules.user.vo.AuthTokenVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证", description = "用户注册和登录")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "创建新用户账号")
    public ResponseEntity<ApiResponse<AuthTokenVO>> register(@Valid @RequestBody UserRegisterDTO dto) {
        return Response.success("User registered successfully")
                .data(userService.register(dto))
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过邮箱和密码登录")
    public ResponseEntity<ApiResponse<AuthTokenVO>> login(@Valid @RequestBody UserLoginDTO dto) {
        return Response.success("Login successful")
                .data(userService.login(dto))
                .build();
    }
}
