package com.metacraft.api.modules.yuanmeng.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.metacraft.api.modules.yuanmeng.ws.YuanMengClientWebSocketHandler;
import com.metacraft.api.modules.yuanmeng.ws.YuanMengNanobotWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class YuanMengWebSocketConfig implements WebSocketConfigurer {

    private final YuanMengClientWebSocketHandler clientHandler;
    private final YuanMengNanobotWebSocketHandler nanobotHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientHandler, "/ws/yuanmeng/client")
                .setAllowedOriginPatterns("*");
        registry.addHandler(nanobotHandler, "/ws/yuanmeng/nanobot")
                .setAllowedOriginPatterns("*");
    }
}