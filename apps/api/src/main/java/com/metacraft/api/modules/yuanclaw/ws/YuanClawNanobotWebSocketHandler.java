package com.metacraft.api.modules.yuanclaw.ws;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.yuanclaw.dto.YuanClawWsMessage;
import com.metacraft.api.modules.yuanclaw.service.YuanClawBridgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class YuanClawNanobotWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final YuanClawBridgeService bridgeService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        bridgeService.registerNanobot(session);
        bridgeService.sendToSession(session, YuanClawWsMessage.builder()
                .type("hello")
                .chatId(bridgeService.getSharedRoom())
                .build());
        bridgeService.broadcastStatus();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        YuanClawWsMessage payload = objectMapper.readValue(message.getPayload(), YuanClawWsMessage.class);
        if ("auth".equals(payload.getType())) {
            if (!bridgeService.validateNanobotToken(payload.getContent())) {
                bridgeService.sendToSession(session, YuanClawWsMessage.builder()
                        .type("error")
                        .content("invalid nanobot token")
                        .build());
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));
                return;
            }
            bridgeService.sendToSession(session, YuanClawWsMessage.builder()
                    .type("auth_ok")
                    .chatId(bridgeService.getSharedRoom())
                    .build());
            return;
        }

        bridgeService.handleNanobotMessage(payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("YuanClaw nanobot transport error {}: {}", session.getId(), exception.getMessage());
        closeQuietly(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        bridgeService.unregisterNanobot(session);
        bridgeService.broadcastStatus();
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
        }
    }
}