package com.metacraft.api.modules.yuanclaw.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.yuanclaw.config.YuanClawProperties;
import com.metacraft.api.modules.yuanclaw.dto.YuanClawWsMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class YuanClawBridgeService {

    private final ObjectMapper objectMapper;
    private final YuanClawProperties properties;

    private final Set<WebSocketSession> clientSessions = ConcurrentHashMap.newKeySet();
    private volatile WebSocketSession nanobotSession;

    public void registerClient(WebSocketSession session) {
        clientSessions.add(session);
        log.info("YuanClaw client connected: {}", session.getId());
    }

    public void unregisterClient(WebSocketSession session) {
        clientSessions.remove(session);
        log.info("YuanClaw client disconnected: {}", session.getId());
    }

    public void registerNanobot(WebSocketSession session) {
        this.nanobotSession = session;
        log.info("YuanClaw nanobot connected: {}", session.getId());
    }

    public void unregisterNanobot(WebSocketSession session) {
        if (session != null && session.equals(this.nanobotSession)) {
            this.nanobotSession = null;
            log.info("YuanClaw nanobot disconnected: {}", session.getId());
        }
    }

    public boolean isNanobotConnected() {
        WebSocketSession session = nanobotSession;
        return session != null && session.isOpen();
    }

    public int getClientCount() {
        return clientSessions.size();
    }

    public String getSharedRoom() {
        return properties.getSharedRoom();
    }

    public boolean validateNanobotToken(String token) {
        String expected = properties.getNanobotToken();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equals(token);
    }

    public void handleClientMessage(WebSocketSession session, YuanClawWsMessage incoming) throws IOException {
        String content = incoming.getContent();
        if (content == null || content.isBlank()) {
            sendToSession(session, YuanClawWsMessage.builder()
                    .type("error")
                    .content("content is required")
                    .build());
            return;
        }

        String chatId = resolveChatId(incoming.getChatId());
        String senderId = resolveSenderId(incoming.getSenderId(), session);
        Map<String, Object> metadata = incoming.getMetadata() == null
                ? new ConcurrentHashMap<>()
                : new ConcurrentHashMap<>(incoming.getMetadata());
        metadata.putIfAbsent("source", "client");

        YuanClawWsMessage clientEcho = YuanClawWsMessage.builder()
                .type("user_message")
                .chatId(chatId)
                .senderId(senderId)
                .content(content)
                .metadata(metadata)
                .build();
        broadcast(clientEcho);

        if (!isNanobotConnected()) {
            sendToSession(session, YuanClawWsMessage.builder()
                    .type("error")
                    .chatId(chatId)
                    .content("nanobot is offline")
                    .build());
            return;
        }

        YuanClawWsMessage outbound = YuanClawWsMessage.builder()
                .type("inbound")
                .chatId(chatId)
                .senderId(senderId)
                .content(content)
                .metadata(metadata)
                .build();
        sendToNanobot(outbound);
    }

    public void handleNanobotMessage(YuanClawWsMessage incoming) throws IOException {
        String type = incoming.getType();
        if (type == null || type.isBlank()) {
            return;
        }

        YuanClawWsMessage outbound = switch (type) {
            case "outbound" -> YuanClawWsMessage.builder()
                    .type("assistant_message")
                    .chatId(resolveChatId(incoming.getChatId()))
                    .content(incoming.getContent())
                    .replyTo(incoming.getReplyTo())
                    .metadata(incoming.getMetadata())
                    .build();
            case "progress" -> YuanClawWsMessage.builder()
                    .type("progress")
                    .chatId(resolveChatId(incoming.getChatId()))
                    .content(incoming.getContent())
                    .metadata(incoming.getMetadata())
                    .build();
            case "pong", "auth_ok" -> incoming;
            default -> YuanClawWsMessage.builder()
                    .type(type)
                    .chatId(resolveChatId(incoming.getChatId()))
                    .content(incoming.getContent())
                    .metadata(incoming.getMetadata())
                    .build();
        };

        if ("pong".equals(outbound.getType()) || "auth_ok".equals(outbound.getType())) {
            WebSocketSession session = nanobotSession;
            if (session != null && session.isOpen()) {
                sendToSession(session, outbound);
            }
            return;
        }

        broadcast(outbound);
    }

    public void broadcastStatus() {
        YuanClawWsMessage status = YuanClawWsMessage.builder()
                .type("status")
                .chatId(properties.getSharedRoom())
                .metadata(Map.of(
                        "nanobotConnected", isNanobotConnected(),
                        "clientCount", getClientCount()))
                .build();
        broadcast(status);
    }

    private void sendToNanobot(YuanClawWsMessage message) throws IOException {
        WebSocketSession session = nanobotSession;
        if (session == null || !session.isOpen()) {
            throw new IOException("nanobot websocket is not connected");
        }
        sendToSession(session, message);
    }

    private void broadcast(YuanClawWsMessage message) {
        clientSessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : clientSessions) {
            try {
                sendToSession(session, message);
            } catch (IOException e) {
                log.warn("Failed to push YuanClaw message to client {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    public void sendToSession(WebSocketSession session, YuanClawWsMessage message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(toJson(message)));
        }
    }

    private String toJson(YuanClawWsMessage message) throws JsonProcessingException {
        return objectMapper.writeValueAsString(message);
    }

    private String resolveChatId(String chatId) {
        return (chatId == null || chatId.isBlank()) ? properties.getSharedRoom() : chatId;
    }

    private String resolveSenderId(String senderId, WebSocketSession session) {
        if (senderId != null && !senderId.isBlank()) {
            return senderId;
        }
        return "client-" + session.getId();
    }
}