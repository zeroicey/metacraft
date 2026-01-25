package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
import com.metacraft.api.modules.ai.vo.ChatMessageVO;
import com.metacraft.api.modules.ai.vo.ChatSessionVO;
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

@RestController
@RequestMapping("/ai/sessions")
@Tag(name = "AI Chat Sessions", description = "Operations for managing chat sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    @Operation(summary = "Create a new chat session")
    public ResponseEntity<ApiResponse<ChatSessionVO>> createSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatSessionCreateDTO dto) {
        return Response.success("Session created successfully")
                .data(chatSessionService.createSession(userDetails.getId(), dto))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all chat sessions for the current user")
    public ResponseEntity<ApiResponse<List<ChatSessionVO>>> getUserSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return Response.success("Sessions retrieved successfully")
                .data(chatSessionService.getUserSessions(userDetails.getId()))
                .build();
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a chat session by ID")
    public ResponseEntity<ApiResponse<ChatSessionVO>> getSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        return Response.success("Session retrieved successfully")
                .data(chatSessionService.getSession(userDetails.getId(), sessionId))
                .build();
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "Update a chat session")
    public ResponseEntity<ApiResponse<ChatSessionVO>> updateSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatSessionUpdateDTO dto) {
        return Response.success("Session updated successfully")
                .data(chatSessionService.updateSession(userDetails.getId(), sessionId, dto))
                .build();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a chat session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        chatSessionService.deleteSession(userDetails.getId(), sessionId);
        return Response.success("Session deleted successfully").build();
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Get all messages for a chat session")
    public ResponseEntity<ApiResponse<List<ChatMessageVO>>> getSessionMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String sessionId) {
        return Response.success("Messages retrieved successfully")
                .data(chatMessageService.getMessagesBySessionId(userDetails.getId(), sessionId))
                .build();
    }
}
