package com.metacraft.api.modules.yuanclaw.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.metacraft.api.modules.yuanclaw.ws.YuanClawClientWebSocketHandler;
import com.metacraft.api.modules.yuanclaw.ws.YuanClawNanobotWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class YuanClawWebSocketConfig implements WebSocketConfigurer {

    private final YuanClawClientWebSocketHandler clientHandler;
    private final YuanClawNanobotWebSocketHandler nanobotHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientHandler, "/ws/yuanclaw/client")
                .setAllowedOriginPatterns("*");
        registry.addHandler(nanobotHandler, "/ws/yuanclaw/nanobot")
                .setAllowedOriginPatterns("*");
    }
}