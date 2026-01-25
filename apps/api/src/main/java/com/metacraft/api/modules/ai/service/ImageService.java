package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import ai.z.openapi.service.model.SensitiveWordCheckRequest;
import com.metacraft.api.modules.ai.dto.GenerateLogoRequestDTO;
import com.metacraft.api.modules.ai.vo.GenerateLogoResponseVO;
import com.metacraft.api.modules.ai.prompt.ImagePrompts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImageService {

    @Value("${zhipuai.api-key}")
    private String apiKey;

    private ZhipuAiClient client;

    @PostConstruct
    public void init() {
        this.client = ZhipuAiClient.builder()
                .ofZHIPU()
                .apiKey(apiKey)
                .enableTokenCache()
                .build();
    }

    /**
     * 生成 App Logo
     */
    public GenerateLogoResponseVO generateLogo(GenerateLogoRequestDTO request) {
        String prompt = ImagePrompts.buildLogoPrompt(request.getName(), request.getDescription());

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
        String imageUrl = response.getData().getData().get(0).getUrl();

        return new GenerateLogoResponseVO(imageUrl, null);
    }

    
}
