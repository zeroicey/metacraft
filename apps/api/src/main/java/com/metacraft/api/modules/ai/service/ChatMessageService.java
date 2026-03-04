package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.ChatMessageCreateDTO;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.entity.ChatSessionEntity;
import com.metacraft.api.modules.ai.repository.ChatMessageRepository;
import com.metacraft.api.modules.ai.repository.ChatSessionRepository;
import com.metacraft.api.modules.ai.vo.ChatMessageVO;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.repository.AppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

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
         ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));
         
         if (!session.getUserId().equals(userId)) {
             throw new IllegalArgumentException("Access denied to this session");
        }

        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
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
            });
        }

        return builder.build();
    }
}
