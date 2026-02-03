package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.service.AgentIntentService;
import com.metacraft.api.modules.ai.service.AgentService;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import com.metacraft.api.security.JwtTokenProvider;
import com.metacraft.api.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/agent")
@Tag(name = "AI 智能体", description = "AI 智能体相关接口(仅流式输出)")
public class AgentController {

    private final AgentIntentService agentIntentService;
    private final AgentService agentService;
    private final JwtTokenProvider jwtTokenProvider;

    public AgentController(AgentIntentService agentIntentService, AgentService agentService, JwtTokenProvider jwtTokenProvider) {
        this.agentIntentService = agentIntentService;
        this.agentService = agentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    @PostMapping(value = "/unified")
    @Operation(summary = "智能体统一入口", description = "SSE 流式统一入口，自动识别意图并流式返回")
    public Object unified(
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        return agentService.unifiedStream(request, userId);
    }
}
