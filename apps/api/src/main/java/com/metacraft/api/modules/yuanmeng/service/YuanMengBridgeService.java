package com.metacraft.api.modules.yuanmeng.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metacraft.api.modules.yuanmeng.config.YuanMengProperties;
import com.metacraft.api.modules.yuanmeng.dto.YuanMengWsMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class YuanMengBridgeService {

    private final ObjectMapper objectMapper;
    private final YuanMengProperties properties;

    private final Set<WebSocketSession> clientSessions = ConcurrentHashMap.newKeySet();
    private volatile WebSocketSession nanobotSession;

    public void registerClient(WebSocketSession session) {
        clientSessions.add(session);
        log.info("YuanMeng client connected: {}", session.getId());
    }

    public void unregisterClient(WebSocketSession session) {
        clientSessions.remove(session);
        log.info("YuanMeng client disconnected: {}", session.getId());
    }

    public void registerNanobot(WebSocketSession session) {
        this.nanobotSession = session;
        log.info("YuanMeng nanobot connected: {}", session.getId());
    }

    public void unregisterNanobot(WebSocketSession session) {
        if (session != null && session.equals(this.nanobotSession)) {
            this.nanobotSession = null;
            log.info("YuanMeng nanobot disconnected: {}", session.getId());
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

    public void handleClientMessage(WebSocketSession session, YuanMengWsMessage incoming) throws IOException {
        String content = incoming.getContent();
        if (content == null || content.isBlank()) {
            sendToSession(session, YuanMengWsMessage.builder()
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

        YuanMengWsMessage clientEcho = YuanMengWsMessage.builder()
                .type("user_message")
                .chatId(chatId)
                .senderId(senderId)
                .content(content)
                .metadata(metadata)
                .build();
        broadcast(clientEcho);

        if (!isNanobotConnected()) {
            sendToSession(session, YuanMengWsMessage.builder()
                    .type("error")
                    .chatId(chatId)
                    .content("nanobot is offline")
                    .build());
            return;
        }

        YuanMengWsMessage outbound = YuanMengWsMessage.builder()
                .type("inbound")
                .chatId(chatId)
                .senderId(senderId)
                .content(content)
                .metadata(metadata)
                .build();
        sendToNanobot(outbound);
    }

    public void handleNanobotMessage(YuanMengWsMessage incoming) throws IOException {
        String type = incoming.getType();
        if (type == null || type.isBlank()) {
            return;
        }

        YuanMengWsMessage outbound = switch (type) {
            case "outbound" -> YuanMengWsMessage.builder()
                    .type("assistant_message")
                    .chatId(resolveChatId(incoming.getChatId()))
                    .content(incoming.getContent())
                    .replyTo(incoming.getReplyTo())
                    .metadata(incoming.getMetadata())
                    .build();
            case "progress" -> YuanMengWsMessage.builder()
                    .type("progress")
                    .chatId(resolveChatId(incoming.getChatId()))
                    .content(incoming.getContent())
                    .metadata(incoming.getMetadata())
                    .build();
            case "pong", "auth_ok" -> incoming;
            default -> YuanMengWsMessage.builder()
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
        YuanMengWsMessage status = YuanMengWsMessage.builder()
                .type("status")
                .chatId(properties.getSharedRoom())
                .metadata(Map.of(
                        "nanobotConnected", isNanobotConnected(),
                        "clientCount", getClientCount()))
                .build();
        broadcast(status);
    }

    private void sendToNanobot(YuanMengWsMessage message) throws IOException {
        WebSocketSession session = nanobotSession;
        if (session == null || !session.isOpen()) {
            throw new IOException("nanobot websocket is not connected");
        }
        sendToSession(session, message);
    }

    private void broadcast(YuanMengWsMessage message) {
        clientSessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : clientSessions) {
            try {
                sendToSession(session, message);
            } catch (IOException e) {
                log.warn("Failed to push YuanMeng message to client {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    public void sendToSession(WebSocketSession session, YuanMengWsMessage message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(toJson(message)));
        }
    }

    private String toJson(YuanMengWsMessage message) throws JsonProcessingException {
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