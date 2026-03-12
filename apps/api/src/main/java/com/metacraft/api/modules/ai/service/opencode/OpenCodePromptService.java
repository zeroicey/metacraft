package com.metacraft.api.modules.ai.service.opencode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class OpenCodePromptService {

    public String buildAppGenPrompt(String targetDir) {
        return render("prompts/app-gen.txt", Map.of(
                "targetDir", targetDir,
                "targetIndex", targetDir + "/index.html",
                "targetScript", targetDir + "/app.js"));
    }

    public String buildAppEditPrompt(String sourceDir, String targetDir) {
        return render("prompts/app-edit.txt", Map.of(
                "sourceDir", sourceDir,
                "targetDir", targetDir,
                "targetIndex", targetDir + "/index.html",
                "targetScript", targetDir + "/app.js"));
    }

    private String render(String classpathLocation, Map<String, String> variables) {
        String template = load(classpathLocation);
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
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