package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.dto.AgentIntentRequestDTO;
import com.metacraft.api.modules.ai.service.AgentService;
import com.metacraft.api.modules.ai.vo.AgentIntentResponseVO;
import com.metacraft.api.modules.ai.vo.PlanResponseVO;
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

    private final AgentService aiAgentService;
    private final JwtTokenProvider jwtTokenProvider;

    public AgentController(AgentService aiAgentService, JwtTokenProvider jwtTokenProvider) {
        this.aiAgentService = aiAgentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping(value = "/intent")
    @Operation(summary = "判断用户意图", description = "返回 chat 或 gen，非流式")
    public ResponseEntity<ApiResponse<AgentIntentResponseVO>> classifyIntent(
            @Valid @RequestBody AgentIntentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        AgentIntentResponseVO intent = aiAgentService.classifyIntent(request);
        return Response.success("Intent classified")
                .data(intent)
                .build();
    }

    @PostMapping(value = "/chat")
    @Operation(summary = "智能体聊天", description = "SSE 流式聊天")
    public Object chat(
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        return aiAgentService.chatStream(request);
    }

    @PostMapping(value = "/plan")
    @Operation(summary = "智能体规划", description = "返回规划文本，非流式")
    public ResponseEntity<ApiResponse<PlanResponseVO>> plan(
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        PlanResponseVO vo = aiAgentService.plan(request);
        return Response.success("Plan completed")
                .data(vo)
                .build();
    }

    @PostMapping(value = "/gen")
    @Operation(summary = "智能体生成", description = "SSE 流式生成代码/页面/工具")
    public Object gen(
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        return aiAgentService.genStream(request);
    }
}
