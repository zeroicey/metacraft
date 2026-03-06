package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import com.metacraft.api.modules.ai.dto.GenerateLogoRequestDTO;
import com.metacraft.api.modules.ai.vo.GenerateLogoResponseVO;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ImageService {

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("classpath:prompts/image-logo-gen.txt")
    private Resource logoPromptTemplateResource;

    private ZhipuAiClient client;
    private String logoPromptTemplate;

    @PostConstruct
    public void init() {
        this.client = ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .enableTokenCache()
                .build();

        try {
            byte[] bytes = logoPromptTemplateResource.getInputStream().readAllBytes();
            this.logoPromptTemplate = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 Logo 提示词模板失败", e);
        }
    }

    /**
     * 生成 App Logo
     */
    public GenerateLogoResponseVO generateLogo(GenerateLogoRequestDTO request) {
        String prompt = logoPromptTemplate
            .replace("{{appName}}", request.getName())
            .replace("{{appDescription}}", request.getDescription());

        // 创建图片生成请求
        CreateImageRequest imageRequest = CreateImageRequest.builder()
                .model("glm-image")
                .prompt(prompt)
                .size("1024x1024")
                .watermarkEnabled(false)
                .build();

        // 调用智谱 AI 图片生成
        ImageResponse response = client.images().createImage(imageRequest);

        if (!response.isSuccess()) {
            throw new RuntimeException("图片生成失败: " + response.getMsg());
        }

        // 提取生成的图片 URL
        String imageUrl = response.getData().getData().getFirst().getUrl();

        return new GenerateLogoResponseVO(imageUrl, null);
    }


}
