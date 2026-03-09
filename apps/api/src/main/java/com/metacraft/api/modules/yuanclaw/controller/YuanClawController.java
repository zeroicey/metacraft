package com.metacraft.api.modules.yuanclaw.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metacraft.api.modules.yuanclaw.service.YuanClawBridgeService;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/yuanclaw")
@RequiredArgsConstructor
public class YuanClawController {

    private final YuanClawBridgeService bridgeService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        return Response.success("YuanClaw status")
                .data(Map.of(
                        "sharedRoom", bridgeService.getSharedRoom(),
                        "clientCount", bridgeService.getClientCount(),
                        "nanobotConnected", bridgeService.isNanobotConnected()))
                .build();
    }
}