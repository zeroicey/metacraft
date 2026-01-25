package com.metacraft.api.modules.ai.controller;

import com.metacraft.api.modules.ai.dto.GenerateLogoRequestDTO;
import com.metacraft.api.modules.ai.service.ImageService;
import com.metacraft.api.modules.ai.vo.GenerateLogoResponseVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/image")
@Tag(name = "AI 图片生成", description = "AI 图片生成相关接口")
public class ImageController {

    private final ImageService aiImageService;

    public ImageController(ImageService aiImageService) {
        this.aiImageService = aiImageService;
    }

    @PostMapping("/generate-logo")
    @Operation(summary = "生成 App Logo", description = "根据应用名称和描述生成 App Logo")
    public ResponseEntity<ApiResponse<GenerateLogoResponseVO>> generateLogo(
            @Valid @RequestBody GenerateLogoRequestDTO request) {
        GenerateLogoResponseVO response = aiImageService.generateLogo(request);
        return Response.success("Logo generated successfully")
                .data(response)
                .build();
    }
}
