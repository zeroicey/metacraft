package com.metacraft.api.modules.ai.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.entity.ChatSessionEntity;
import com.metacraft.api.modules.ai.repository.ChatMessageRepository;
import com.metacraft.api.modules.ai.repository.ChatSessionRepository;
import com.metacraft.api.modules.ai.vo.ChatMessageVO;
import com.metacraft.api.modules.app.repository.AppRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final String NO_HISTORY = "无历史对话。";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AppRepository appRepository;

    @Transactional
    public ChatMessageVO saveMessage(Long userId, ChatMessageCreateDTO dto) {
        // Validate session exists
        ChatSessionEntity session = chatSessionRepository.findBySessionId(dto.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + dto.getSessionId()));
        
        if (!session.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Access denied to this session");
        }

        ChatMessageEntity message = ChatMessageEntity.builder()
                .userId(userId)
                .sessionId(dto.getSessionId())
                .role(dto.getRole())
                .content(dto.getContent())
                .type(dto.getType() != null ? dto.getType() : "text")
                .relatedAppId(dto.getRelatedAppId())
                .relatedVersionId(dto.getRelatedVersionId())
                .build();
        
        ChatMessageEntity saved = chatMessageRepository.save(message);
        
        // Update session timestamp
        // Assuming JPA/Hibernate @UpdateTimestamp works on entity update. 
        // But here we are touching Session just to check. 
        // Ideally we should update session's updated_at.
        // session.setUpdatedAt(LocalDateTime.now()); 
        // chatSessionRepository.save(session); 
        // Not strictly required for basic CRUD but good practice.
        
        return convertToVO(saved);
    }

    public List<ChatMessageVO> getMessagesBySessionId(Long userId, String sessionId) {
         getOwnedSession(userId, sessionId);

        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    public String buildRecentConversationHistory(Long userId, String sessionId, int limit) {
        if (limit <= 0) {
            return NO_HISTORY;
        }

        getOwnedSession(userId, sessionId);

        String history = chatMessageRepository
                .findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, limit))
                .stream()
                .sorted(Comparator.comparing(ChatMessageEntity::getCreatedAt)
                        .thenComparing(ChatMessageEntity::getId))
                .map(this::formatHistoryMessage)
                .filter(message -> !message.isBlank())
                .collect(Collectors.joining("\n\n"));

        return history.isBlank() ? NO_HISTORY : history;
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        ChatMessageEntity message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this message");
        }

        chatMessageRepository.delete(message);
    }

    private ChatSessionEntity getOwnedSession(Long userId, String sessionId) {
        ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this session");
        }

        return session;
    }

    private String formatHistoryMessage(ChatMessageEntity entity) {
        String content = entity.getContent() == null ? "" : entity.getContent().trim();
        if (content.isBlank()) {
            return "";
        }

        String roleLabel = switch (entity.getRole()) {
            case "user" -> "用户";
            case "assistant" -> "助手";
            default -> entity.getRole();
        };

        String typeSuffix = entity.getType() != null && !"text".equalsIgnoreCase(entity.getType())
                ? "|" + entity.getType()
                : "";

        return "[" + roleLabel + typeSuffix + "]\n" + content;
    }

    private ChatMessageVO convertToVO(ChatMessageEntity entity) {
        ChatMessageVO.ChatMessageVOBuilder builder = ChatMessageVO.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .role(entity.getRole())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .type(entity.getType())
                .relatedAppId(entity.getRelatedAppId())
                .relatedVersionId(entity.getRelatedVersionId());

        // If message has related app, fetch app details
        if (entity.getRelatedAppId() != null) {
            appRepository.findById(entity.getRelatedAppId()).ifPresent(app -> {
                builder.relatedAppUuid(app.getUuid());
                builder.relatedAppName(app.getName());
                builder.relatedAppDescription(app.getDescription());
                builder.relatedAppLogo(app.getLogo());
            });
        }

        return builder.build();
    }
}
