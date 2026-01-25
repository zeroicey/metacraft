package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.service.ChatMessageService;
import com.metacraft.api.modules.ai.vo.ChatMessageVO;
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

@RestController
@RequestMapping("/ai/messages")
@Tag(name = "AI Chat Messages", description = "Operations for managing chat messages (manual CRUD)")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    @Operation(summary = "Manually add a message to a session")
    public ResponseEntity<ApiResponse<ChatMessageVO>> createMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatMessageCreateDTO dto) {
        return Response.success("Message created successfully")
                .data(chatMessageService.saveMessage(userDetails.getId(), dto))
                .build();
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete a specific message")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long messageId) {
        chatMessageService.deleteMessage(userDetails.getId(), messageId);
        return Response.success("Message deleted successfully").build();
    }
}
