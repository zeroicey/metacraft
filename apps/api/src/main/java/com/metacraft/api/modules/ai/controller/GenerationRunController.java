package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.AgentRequestDTO;
import com.metacraft.api.modules.ai.entity.GenerationRunEntity;
import com.metacraft.api.modules.ai.service.AgentService;
import com.metacraft.api.modules.ai.service.GenerationRunService;
import com.metacraft.api.modules.ai.vo.GenerationRunVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import com.metacraft.api.security.AuthUtils;
import com.metacraft.api.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/ai/runs")
@Tag(name = "AI Generation Runs", description = "AI run resources and streaming interface")
@RequiredArgsConstructor
public class GenerationRunController {

    private final AgentService agentService;
    private final GenerationRunService generationRunService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @Operation(summary = "Create and start a generation run", description = "Creates a runId and returns SSE stream")
    public Flux<ServerSentEvent<String>> createRun(
            @Valid @RequestBody AgentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        String runId = UUID.randomUUID().toString();
        generationRunService.createRun(runId, userId, request.getSessionId());

        return agentService.run(request, userId, runId);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get run status", description = "Returns one run status and summary")
    public ResponseEntity<ApiResponse<GenerationRunVO>> getRun(
            @PathVariable String runId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        GenerationRunEntity run = generationRunService.getRun(runId, userId);
        GenerationRunVO vo = GenerationRunVO.builder()
                .runId(run.getRunId())
                .sessionId(run.getSessionId())
                .intent(run.getIntent())
                .status(run.getStatus())
                .errorMessage(run.getErrorMessage())
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt())
                .completedAt(run.getCompletedAt())
                .build();

        return Response.success("Run retrieved successfully")
                .data(vo)
                .build();
    }

    @PostMapping("/{runId}/cancel")
    @Operation(summary = "Cancel run", description = "Marks run as cancelled")
    public ResponseEntity<ApiResponse<Void>> cancelRun(
            @PathVariable String runId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthUtils.validateAuthorization(authHeader, jwtTokenProvider);
        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        generationRunService.cancelRun(runId, userId);

        return Response.success("Run cancelled successfully").build();
    }
}
