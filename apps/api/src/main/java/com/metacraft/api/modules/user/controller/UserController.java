package cc.serenique.api.modules.user.controller;

import cc.serenique.api.modules.user.dto.UserUpdateDTO;
import cc.serenique.api.modules.user.service.UserService;
import cc.serenique.api.modules.user.vo.UserVO;
import cc.serenique.api.response.ApiResponse;
import cc.serenique.api.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户信息的查询和更新相关接口")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get user information", description = "Retrieves the current user information.")
    public ResponseEntity<ApiResponse<UserVO>> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        return Response.success("User information retrieved successfully")
                .data(userService.getCurrentUser(userDetails.getUsername()))
                .build();
    }

    @PatchMapping
    @Operation(summary = "Update user information", description = "Partially updates the user profile.")
    public ResponseEntity<ApiResponse<UserVO>> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateDTO updateDTO) {
        return Response.success("User information updated successfully")
                .data(userService.updateUser(userDetails.getUsername(), updateDTO))
                .build();
    }
}
