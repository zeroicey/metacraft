package com.metacraft.api.modules._ai.controller;

import com.metacraft.api.modules._ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules._ai.dto.ChatSessionUpdateDTO;
import com.metacraft.api.modules._ai.service.ChatMessageService;
import com.metacraft.api.modules._ai.service.ChatSessionService;
import com.metacraft.api.modules._ai.vo.ChatMessageVO;
import com.metacraft.api.modules._ai.vo.ChatSessionVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import com.metacraft.api.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController
@RequestMapping("/ai/sessions")
@Tag(name = "AI 聊天会话", description = "聊天会话管理相关接口")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    @Operation(summary = "创建新的聊天会话")
    public ResponseEntity<ApiResponse<ChatSessionVO>> createSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatSessionCreateDTO dto) {
        return Response.success("Session created successfully")
                .data(chatSessionService.createSession(userDetails.getId(), dto))
                .build();
    }

    @GetMapping
    @Operation(summary = "获取当前用户的所有聊天会话")
    public ResponseEntity<ApiResponse<List<ChatSessionVO>>> getUserSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return Response.success("Sessions retrieved successfully")
                .data(chatSessionService.getUserSessions(userDetails.getId()))
                .build();
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "根据 ID 获取聊天会话")
    public ResponseEntity<ApiResponse<ChatSessionVO>> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        return Response.success("Session retrieved successfully")
                .data(chatSessionService.getSession(userDetails.getId(), sessionId))
                .build();
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "更新聊天会话")
    public ResponseEntity<ApiResponse<ChatSessionVO>> updateSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatSessionUpdateDTO dto) {
        return Response.success("Session updated successfully")
                .data(chatSessionService.updateSession(userDetails.getId(), sessionId, dto))
                .build();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "删除聊天会话")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        chatSessionService.deleteSession(userDetails.getId(), sessionId);
        return Response.success("Session deleted successfully").build();
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "获取聊天会话的所有消息")
    public ResponseEntity<ApiResponse<List<ChatMessageVO>>> getSessionMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        return Response.success("Messages retrieved successfully")
                .data(chatMessageService.getMessagesBySessionId(userDetails.getId(), sessionId))
                .build();
    }
}
