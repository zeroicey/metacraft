package com.metacraft.api.modules.app.controller;

import com.metacraft.api.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logo")
@RequiredArgsConstructor
public class LogoController {

    private static final String[] EXTS = new String[]{"png", "jpg", "jpeg", "webp"};

    private final StorageService storageService;

    @GetMapping("/{uuid}")
    public ResponseEntity<byte[]> getLogo(@PathVariable String uuid) {
        for (String ext : EXTS) {
            String relativePath = "logos/" + uuid + "." + ext;
            if (!storageService.exists(relativePath)) {
                continue;
            }

            byte[] content = storageService.readBinaryFile(relativePath);
            return ResponseEntity.ok()
                    .contentType(toMediaType(ext))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(content);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private MediaType toMediaType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_PNG;
        };
    }
}
