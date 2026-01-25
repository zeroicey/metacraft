package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.vo.ChatMessageVO;
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

@RestController
@RequestMapping("/ai/messages")
@Tag(name = "AI Chat Messages", description = "Operations for managing chat messages (manual CRUD)")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final UserService userService;

    private Long getUserId(UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername()).getId();
    }

    @PostMapping
    @Operation(summary = "Manually add a message to a session")
    public ResponseEntity<ApiResponse<ChatMessageVO>> createMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatMessageCreateDTO dto) {
        Long userId = getUserId(userDetails);
        return Response.success("Message created successfully")
                .data(chatMessageService.saveMessage(userId, dto))
                .build();
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete a specific message")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long messageId) {
        Long userId = getUserId(userDetails);
        chatMessageService.deleteMessage(userId, messageId);
        return Response.success("Message deleted successfully").build();
    }
}
