package com.metacraft.api.modules.app.controller;

import com.metacraft.api.modules.app.dto.AppCreateDTO;
import com.metacraft.api.modules.app.dto.AppUpdateDTO;
import com.metacraft.api.modules.app.service.AppService;
import com.metacraft.api.modules.app.vo.AppVO;
import com.metacraft.api.modules.app.vo.AppVersionVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import com.metacraft.api.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apps")
@Tag(name = "应用管理", description = "应用和版本的增删改查相关接口")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @PostMapping
    @Operation(summary = "创建新应用")
    public ResponseEntity<ApiResponse<AppVO>> createApp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AppCreateDTO dto) {
        return Response.success("App created successfully")
                .data(appService.createApp(userDetails.getId(), dto))
                .build();
    }

    @GetMapping
    @Operation(summary = "获取当前用户的所有应用")
    public ResponseEntity<ApiResponse<List<AppVO>>> getUserApps(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return Response.success("Apps retrieved successfully")
                .data(appService.getUserApps(userDetails.getId()))
                .build();
    }

    @GetMapping("/{appId}")
    @Operation(summary = "根据 ID 获取应用详情")
    public ResponseEntity<ApiResponse<AppVO>> getAppById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long appId) {
        return Response.success("App retrieved successfully")
                .data(appService.getAppById(userDetails.getId(), appId))
                .build();
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "根据 UUID 获取应用详情")
    public ResponseEntity<ApiResponse<AppVO>> getAppByUuid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 UUID") @PathVariable String uuid) {
        return Response.success("App retrieved successfully")
                .data(appService.getAppByUuid(userDetails.getId(), uuid))
                .build();
    }

    @PatchMapping("/{appId}")
    @Operation(summary = "更新应用信息")
    public ResponseEntity<ApiResponse<AppVO>> updateApp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long appId,
            @Valid @RequestBody AppUpdateDTO dto) {
        return Response.success("App updated successfully")
                .data(appService.updateApp(userDetails.getId(), appId, dto))
                .build();
    }

    @DeleteMapping("/{appId}")
    @Operation(summary = "删除应用（包括所有版本和文件）")
    public ResponseEntity<ApiResponse<Void>> deleteApp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long appId) {
        appService.deleteApp(userDetails.getId(), appId);
        return Response.success("App deleted successfully").build();
    }

    @GetMapping("/{appId}/versions")
    @Operation(summary = "获取应用的所有版本")
    public ResponseEntity<ApiResponse<List<AppVersionVO>>> getAppVersions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long appId) {
        return Response.success("Versions retrieved successfully")
                .data(appService.getAppVersions(userDetails.getId(), appId))
                .build();
    }

    @GetMapping("/{appId}/versions/{versionNumber}")
    @Operation(summary = "获取指定版本的详情")
    public ResponseEntity<ApiResponse<AppVersionVO>> getVersion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long appId,
            @Parameter(description = "版本号") @PathVariable Integer versionNumber) {
        return Response.success("Version retrieved successfully")
                .data(appService.getVersion(userDetails.getId(), appId, versionNumber))
                .build();
    }

    @DeleteMapping("/{appId}/versions/{versionId}")
    @Operation(summary = "删除指定版本（包括文件）")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long appId,
            @Parameter(description = "版本 ID") @PathVariable Long versionId) {
        appService.deleteVersion(userDetails.getId(), appId, versionId);
        return Response.success("Version deleted successfully").build();
    }
}
