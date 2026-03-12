package com.metacraft.api.modules.ai.service.opencode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class OpenCodeTemplateService {

    private static final String INDEX_TEMPLATE = "templates/opencode/index.html";
    private static final String APP_JS_TEMPLATE = "templates/opencode/app.js";

    public String loadIndexHtmlTemplate() {
        return load(INDEX_TEMPLATE);
    }

    public String loadAppJsTemplate() {
        return load(APP_JS_TEMPLATE);
    }

    private String load(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(Objects.requireNonNull(classpathLocation));
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load classpath resource: " + classpathLocation, exception);
        }
    }
}