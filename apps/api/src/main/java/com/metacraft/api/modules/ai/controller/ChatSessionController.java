package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.service.ChatSessionService;
import com.metacraft.api.modules.ai.vo.ChatMessageVO;
import com.metacraft.api.modules.ai.vo.ChatSessionVO;
import com.metacraft.api.modules.user.service.UserService;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/sessions")
@Tag(name = "AI Chat Sessions", description = "Operations for managing chat sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    private Long getUserId(UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername()).getId();
    }

    @PostMapping
    @Operation(summary = "Create a new chat session")
    public ResponseEntity<ApiResponse<ChatSessionVO>> createSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatSessionCreateDTO dto) {
        Long userId = getUserId(userDetails);
        return Response.success("Session created successfully")
                .data(chatSessionService.createSession(userId, dto))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all chat sessions for the current user")
    public ResponseEntity<ApiResponse<List<ChatSessionVO>>> getUserSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return Response.success("Sessions retrieved successfully")
                .data(chatSessionService.getUserSessions(userId))
                .build();
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a chat session by ID")
    public ResponseEntity<ApiResponse<ChatSessionVO>> getSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        Long userId = getUserId(userDetails);
        return Response.success("Session retrieved successfully")
                .data(chatSessionService.getSession(userId, sessionId))
                .build();
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "Update a chat session")
    public ResponseEntity<ApiResponse<ChatSessionVO>> updateSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatSessionUpdateDTO dto) {
        Long userId = getUserId(userDetails);
        return Response.success("Session updated successfully")
                .data(chatSessionService.updateSession(userId, sessionId, dto))
                .build();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a chat session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        Long userId = getUserId(userDetails);
        chatSessionService.deleteSession(userId, sessionId);
        return Response.success("Session deleted successfully").build();
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(summary = "Get all messages for a chat session")
    public ResponseEntity<ApiResponse<List<ChatMessageVO>>> getSessionMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        Long userId = getUserId(userDetails);
        return Response.success("Messages retrieved successfully")
                .data(chatMessageService.getMessagesBySessionId(userId, sessionId))
                .build();
    }
}
