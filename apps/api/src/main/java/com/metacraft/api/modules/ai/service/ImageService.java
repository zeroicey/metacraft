package com.metacraft.api.modules.ai.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.image.CreateImageRequest;
import ai.z.openapi.service.image.ImageResponse;
import com.metacraft.api.modules.ai.dto.GenerateLogoRequestDTO;
import com.metacraft.api.modules.ai.vo.GenerateLogoResponseVO;
import com.metacraft.api.modules.storage.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

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

    /**
     * 异步触发 logo 生成和下载落盘，不阻塞主流程。
     */
    public void generateAndSaveLogoAsync(String name, String description, String logoUuid) {
        CompletableFuture.runAsync(() -> {
            try {
                GenerateLogoRequestDTO request = new GenerateLogoRequestDTO();
                request.setName(name);
                request.setDescription(description);

                GenerateLogoResponseVO response = generateLogo(request);
                String imageUrl = response.getImageUrl();
                if (imageUrl == null || imageUrl.isBlank()) {
                    log.warn("Logo generation succeeded but image URL is empty, uuid={}", logoUuid);
                    return;
                }

                downloadAndStoreLogo(imageUrl, logoUuid);
            } catch (Exception e) {
                log.warn("Async logo task failed, uuid={}", logoUuid, e);
            }
        });
    }

    private void downloadAndStoreLogo(String imageUrl, String logoUuid) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected logo download status: " + response.statusCode());
        }

        byte[] body = response.body();
        if (body == null || body.length == 0) {
            throw new IOException("Downloaded logo is empty");
        }
        if (body.length > MAX_LOGO_SIZE_BYTES) {
            throw new IOException("Downloaded logo exceeds max size");
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        String ext = detectExt(contentType, body);
        String relativePath = "logos/" + logoUuid + "." + ext;
        storageService.saveBinaryFile(relativePath, body);
        log.info("Async logo saved: {}", relativePath);
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
