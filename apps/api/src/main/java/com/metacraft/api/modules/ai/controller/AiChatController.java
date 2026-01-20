package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.ChatRequestDTO;
import com.metacraft.api.modules.ai.service.AiChatService;
import com.metacraft.api.modules.ai.vo.ChatResponseVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import com.metacraft.api.security.JwtTokenProvider;
import com.metacraft.api.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai/chat")
@Tag(name = "AI 聊天", description = "AI 聊天对话相关接口")
public class AiChatController {

    private final AiChatService aiChatService;
    private final JwtTokenProvider jwtTokenProvider;

    public AiChatController(AiChatService aiChatService, JwtTokenProvider jwtTokenProvider) {
        this.aiChatService = aiChatService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/message")
    @Operation(summary = "发送聊天消息", description = "发送消息给 AI 并获取回复,支持流式和非流式响应")
    public Object sendMessage(
            @Valid @RequestBody ChatRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        ResponseEntity<ApiResponse<?>> err = AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        if (err != null) return err;
        
        // 根据 stream 参数决定使用流式还是非流式
        if (Boolean.TRUE.equals(request.getStream())) {
            return aiChatService.chatStream(request);
        } else {
            ChatResponseVO response = aiChatService.chat(request);
            return Response.success("Chat completed successfully")
                    .data(response)
                    .build();
        }
    }
}
