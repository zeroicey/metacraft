package com.metacraft.api.modules.ai.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.metacraft.api.modules.ai.agent.TemplateMatcherAgent;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class TemplateMatcherService {

    private final TemplateMatcherAgent templateMatcherAgent;
    private String templatesPath;

    public TemplateMatcherService(TemplateMatcherAgent templateMatcherAgent) {
        this.templateMatcherAgent = templateMatcherAgent;
    }

    @Value("${app.templates.path:data/templates}")
    public void setTemplatesPath(String templatesPath) {
        this.templatesPath = templatesPath;
    }

    public Mono<String> matchTemplate(String userMessage) {
        List<String> templateNames = scanTemplateDirectories();

        if (templateNames.isEmpty()) {
            log.info("No templates found, fallback to OpenCode");
            return Mono.just(null);
        }

        String templatesList = String.join("\n", templateNames);
        log.info("Matching user message against templates: {}", templateNames);

        return Mono.from(templateMatcherAgent.matchTemplates(templatesList, userMessage))
                .timeout(java.time.Duration.ofSeconds(10))
                .onErrorResume(e -> {
                    log.warn("Template matching error, fallback to OpenCode: {}", e.getMessage());
                    return Mono.just("NONE");
                })
                .map(response -> parseTemplateMatchResponse(response, templateNames));
    }

    private String parseTemplateMatchResponse(String response, List<String> templateNames) {
        String trimmed = response.trim();
        log.info("Template matching response: {}", trimmed);

        if (trimmed.equalsIgnoreCase("NONE")) {
            return null;
        }

        // Validate the returned name exists
        if (templateNames.contains(trimmed)) {
            return trimmed;
        }

        log.warn("Template '{}' not found in available templates", trimmed);
        return null;
    }

    private List<String> scanTemplateDirectories() {
        File templateDir = new File(templatesPath);
        if (!templateDir.exists() || !templateDir.isDirectory()) {
            log.warn("Template directory not found: {}", templatesPath);
            return List.of();
        }

        File[] dirs = templateDir.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) {
            return List.of();
        }

        return Arrays.stream(dirs)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public String getTemplatePath() {
        return templatesPath;
    }
}