package com.metacraft.api.modules.app.service;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class PreviewAssetResolver {

    private static final Set<String> SUPPORTED_ASSET_EXTENSIONS = Set.of("js", "css");

    public String toAssetPath(String htmlPath, String fileName, String extension) {
        String normalizedExtension = normalizeAssetExtension(extension);
        String assetFileName = fileName + "." + normalizedExtension;
        return htmlPath.endsWith("index.html")
                ? htmlPath.substring(0, htmlPath.length() - "index.html".length()) + assetFileName
                : htmlPath + "/" + assetFileName;
    }

    public MediaType resolveAssetMediaType(String extension) {
        String normalizedExtension = normalizeAssetExtension(extension);
        return switch (normalizedExtension) {
            case "js" -> MediaType.valueOf("application/javascript");
            case "css" -> Objects.requireNonNull(MediaType.parseMediaType("text/css"));
            default -> throw new IllegalArgumentException("Unsupported extension: " + extension);
        };
    }

    private String normalizeAssetExtension(String extension) {
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ASSET_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("Unsupported extension: " + extension);
        }
        return normalizedExtension;
    }
}