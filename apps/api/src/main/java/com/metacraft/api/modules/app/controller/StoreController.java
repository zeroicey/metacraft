package com.metacraft.api.modules.app.controller;

import com.metacraft.api.modules.app.dto.CommentRequestDTO;
import com.metacraft.api.modules.app.dto.RatingRequestDTO;
import com.metacraft.api.modules.app.service.StoreService;
import com.metacraft.api.modules.app.vo.StoreAppDetailVO;
import com.metacraft.api.modules.app.vo.StoreAppListVO;
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
@RequestMapping("/api/store/apps")
@Tag(name = "应用商店", description = "应用商店相关接口")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    @Operation(summary = "获取已发布的应用列表")
    public ResponseEntity<ApiResponse<List<StoreAppListVO>>> getStoreApps() {
        return Response.success("Apps retrieved successfully")
                .data(storeService.getPublishedApps())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取应用详情")
    public ResponseEntity<ApiResponse<StoreAppDetailVO>> getAppDetail(
            @Parameter(description = "应用 ID") @PathVariable Long id) {
        return Response.success("App detail retrieved successfully")
                .data(storeService.getAppDetail(id))
                .build();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布应用")
    public ResponseEntity<ApiResponse<Void>> publishApp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long id) {
        storeService.publishApp(id, userDetails.getId());
        return Response.success("App published successfully").build();
    }

    @DeleteMapping("/{id}/publish")
    @Operation(summary = "取消发布应用")
    public ResponseEntity<ApiResponse<Void>> unpublishApp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long id) {
        storeService.unpublishApp(id, userDetails.getId());
        return Response.success("App unpublished successfully").build();
    }

    @PostMapping("/{id}/ratings")
    @Operation(summary = "评分应用")
    public ResponseEntity<ApiResponse<Void>> rateApp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long id,
            @Valid @RequestBody RatingRequestDTO request) {
        storeService.rateApp(id, userDetails.getId(), request);
        return Response.success("Rating submitted successfully").build();
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "添加评论")
    public ResponseEntity<ApiResponse<Void>> addComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long id,
            @Valid @RequestBody CommentRequestDTO request) {
        storeService.addComment(id, userDetails.getId(), request);
        return Response.success("Comment added successfully").build();
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @Operation(summary = "删除评论")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "应用 ID") @PathVariable Long id,
            @Parameter(description = "评论 ID") @PathVariable Long commentId) {
        storeService.deleteComment(commentId, userDetails.getId());
        return Response.success("Comment deleted successfully").build();
    }
}