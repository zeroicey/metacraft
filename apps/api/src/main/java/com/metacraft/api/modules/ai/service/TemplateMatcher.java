package com.metacraft.api.modules.ai.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.ai.dto.TemplateMatchResult;
import com.metacraft.api.modules.ai.entity.Template;

import dev.langchain4j.service.AiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateMatcher {

    @Value("${app.template-path:data/templates}")
    private String templatePath;

    @Value("${app.template-match-timeout:3}")
    private int timeoutSeconds;

    private final TemplateMatchAiService templateMatchAiService;

    private List<Template> templateCache = Collections.emptyList();

    @PostConstruct
    public void init() {
        scanAndLoadTemplates();
    }

    public void scanAndLoadTemplates() {
        try {
            Path templateDir = Paths.get(templatePath).toAbsolutePath().normalize();

            if (!Files.exists(templateDir)) {
                Files.createDirectories(templateDir);
                log.info("Created template directory: {}", templateDir);
                templateCache = Collections.emptyList();
                return;
            }

            try (Stream<Path> paths = Files.walk(templateDir, 1)) {
                templateCache = paths
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(templateDir))
                    .map(this::loadTemplateFromDirectory)
                    .filter(t -> t != null)
                    .toList();
            }

            log.info("Loaded {} templates from {}", templateCache.size(), templatePath);
        } catch (IOException e) {
            log.error("Failed to scan template directory: {}", templatePath, e);
            templateCache = Collections.emptyList();
        }
    }

    private Template loadTemplateFromDirectory(Path dir) {
        try {
            String folderName = dir.getFileName().toString();
            Template template = Template.fromFolderName(folderName);

            Path indexHtml = dir.resolve("index.html");
            if (Files.exists(indexHtml)) {
                template.setHtmlContent(Files.readString(indexHtml));
            }

            Path appJs = dir.resolve("app.js");
            if (Files.exists(appJs)) {
                template.setJsContent(Files.readString(appJs));
            }

            return template;
        } catch (IOException e) {
            log.warn("Failed to load template from {}: {}", dir, e.getMessage());
            return null;
        }
    }

    public List<Template> getTemplates() {
        return Collections.unmodifiableList(templateCache);
    }

    public Mono<TemplateMatchResult> matchAsync(String userMessage) {
        if (templateCache.isEmpty()) {
            return Mono.just(TemplateMatchResult.noMatch());
        }

        String templateList = buildTemplateListPrompt();
        String prompt = String.format("可用模板列表:\n%s\n\n用户需求:\n%s", templateList, userMessage);

        return Flux.from(templateMatchAiService.matchTemplates(prompt))
                .reduce(String::concat)
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::parseMatchResult)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .onErrorResume(e -> {
                    log.warn("Template matching failed: {}", e.getMessage());
                    return Mono.just(TemplateMatchResult.noMatch());
                });
    }

    public Mono<TemplateMatchResult> matchWithTimeout(String userMessage, java.time.Duration timeout) {
        return matchAsync(userMessage)
                .timeout(timeout)
                .onErrorResume(e -> {
                    log.warn("Template matching timed out: {}", e.getMessage());
                    return Mono.just(TemplateMatchResult.noMatch());
                });
    }

    public TemplateMatchResult matchSync(String userMessage) {
        return matchAsync(userMessage).block();
    }

    private String buildTemplateListPrompt() {
        StringBuilder sb = new StringBuilder();
        for (Template template : templateCache) {
            sb.append("- ").append(template.getFolderName())
              .append(" - ").append(template.getDescription()).append("\n");
        }
        return sb.toString();
    }

    private TemplateMatchResult parseMatchResult(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonResponse);

            boolean matched = node.has("matched") && node.get("matched").asBoolean();
            if (!matched) {
                return TemplateMatchResult.noMatch();
            }

            String templateName = node.has("templateName") ? node.get("templateName").asText() : null;
            String reason = node.has("reason") ? node.get("reason").asText() : null;

            Template matchedTemplate = templateCache.stream()
                    .filter(t -> t.getFolderName().equals(templateName))
                    .findFirst()
                    .orElse(null);

            if (matchedTemplate != null) {
                return TemplateMatchResult.matched(matchedTemplate, reason);
            }

            return TemplateMatchResult.noMatch();
        } catch (Exception e) {
            log.warn("Failed to parse template match result: {}", e.getMessage());
            return TemplateMatchResult.noMatch();
        }
    }

    // AI Service 接口
    @AiService
    public interface TemplateMatchAiService {
        @dev.langchain4j.service.SystemMessage(fromResource = "prompts/template-match.txt")
        Flux<String> matchTemplates(@dev.langchain4j.service.UserMessage String prompt);
    }
}