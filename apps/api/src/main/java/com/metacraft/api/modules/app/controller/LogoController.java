package com.metacraft.api.modules.app.controller;

import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metacraft.api.modules.app.service.AppLogoService;
import com.metacraft.api.modules.app.service.AppLogoService.LogoResource;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/logo")
@RequiredArgsConstructor
public class LogoController {

    private final AppLogoService appLogoService;

    @GetMapping("/{uuid}")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> getLogo(@PathVariable String uuid) {
        return appLogoService.findLogoByUuid(uuid)
                .map(this::toResponse)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/app/{appId}")
    @SuppressWarnings("null")
    public ResponseEntity<byte[]> getLogoByAppId(@PathVariable Long appId) {
        return appLogoService.findLogoByAppId(appId)
                .map(this::toResponse)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private ResponseEntity<byte[]> toResponse(LogoResource resource) {
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(resource.mediaType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource.content());
    }
}
