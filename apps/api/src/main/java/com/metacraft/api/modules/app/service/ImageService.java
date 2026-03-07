package com.metacraft.api.modules.app.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

import com.metacraft.api.modules.app.entity.AppEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.dto.AppInfoDTO;
import com.metacraft.api.modules.storage.service.StorageService;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("classpath:prompts/image-logo-gen.txt")
    private Resource logoPromptTemplateResource;

    private static final long MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024;

    private final StorageService storageService;

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
     * 同步生成并保存 App Logo，返回图片扩展名（png/jpg/webp）。
     */
    public String generateLogoAndSave(AppEntity app, String logoUuid) {
        String prompt = logoPromptTemplate
                .replace("{{appName}}", app.getName())
                .replace("{{appDescription}}", app.getDescription());

        // 创建图片生成请求
        CreateImageRequest imageRequest = CreateImageRequest.builder()
                .model("glm-image")
                .prompt(prompt)
                .size("1024x1024")
                .watermarkEnabled(false)
                .build();

        // 调用智谱 AI 图片生成
        ImageResponse imageResponse = client.images().createImage(imageRequest);

        if (!imageResponse.isSuccess()) {
            throw new RuntimeException("图片生成失败: " + imageResponse.getMsg());
        }

        // 提取生成的图片 URL
        String imageUrl = imageResponse.getData().getData().getFirst().getUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new RuntimeException("图片生成成功但返回的图片 URL 为空");
        }

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new IOException("Unexpected logo download status: " + httpResponse.statusCode());
            }

            byte[] body = httpResponse.body();
            if (body == null || body.length == 0) {
                throw new IOException("Downloaded logo is empty");
            }
            if (body.length > MAX_LOGO_SIZE_BYTES) {
                throw new IOException("Downloaded logo exceeds max size");
            }

            String contentType = httpResponse.headers().firstValue("Content-Type").orElse("");
            String ext = detectExt(contentType, body);
            String relativePath = "logos/" + logoUuid + "." + ext;
            storageService.saveBinaryFile(relativePath, body);
            log.info("Logo saved: {}", relativePath);
            return ext;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("下载 Logo 被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("下载并保存 Logo 失败", e);
        }
    }

    private String detectExt(String contentType, byte[] body) {
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("image/png") || isPng(body)) {
            return "png";
        }
        if (normalized.contains("image/jpeg") || normalized.contains("image/jpg") || isJpeg(body)) {
            return "jpg";
        }
        if (normalized.contains("image/webp") || isWebp(body)) {
            return "webp";
        }
        return "png";
    }

    private boolean isPng(byte[] body) {
        return body.length >= 8
                && (body[0] & 0xFF) == 0x89
                && body[1] == 0x50
                && body[2] == 0x4E
                && body[3] == 0x47;
    }

    private boolean isJpeg(byte[] body) {
        return body.length >= 3
                && (body[0] & 0xFF) == 0xFF
                && (body[1] & 0xFF) == 0xD8
                && (body[2] & 0xFF) == 0xFF;
    }

    private boolean isWebp(byte[] body) {
        return body.length >= 12
                && body[0] == 'R'
                && body[1] == 'I'
                && body[2] == 'F'
                && body[3] == 'F'
                && body[8] == 'W'
                && body[9] == 'E'
                && body[10] == 'B'
                && body[11] == 'P';
    }

}
