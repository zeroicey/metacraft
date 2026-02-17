package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.dto.ChatSessionCreateDTO;
import com.metacraft.api.modules.ai.dto.ChatSessionUpdateDTO;
import com.metacraft.api.modules.ai.entity.ChatSessionEntity;
import com.metacraft.api.modules.ai.repository.ChatMessageRepository;
import com.metacraft.api.modules.ai.repository.ChatSessionRepository;
import com.metacraft.api.modules.ai.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatSessionVO createSession(Long userId, ChatSessionCreateDTO dto) {
        ChatSessionEntity session = ChatSessionEntity.builder()
                .userId(userId)
                .sessionId(UUID.randomUUID().toString())
                .title(dto.getTitle())
                .build();
        
        ChatSessionEntity saved = chatSessionRepository.save(session);
        return convertToVO(saved);
    }

    public ChatSessionVO getSession(Long userId, String sessionId) {
        ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this session");
        }
        
        return convertToVO(session);
    }

    public List<ChatSessionVO> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    public ChatSessionVO updateSession(Long userId, String sessionId, ChatSessionUpdateDTO dto) {
        ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this session");
        }
        
        session.setTitle(dto.getTitle());
        ChatSessionEntity updated = chatSessionRepository.save(session);
        return convertToVO(updated);
    }

    @Transactional
    public void deleteSession(Long userId, String sessionId) {
        ChatSessionEntity session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found: " + sessionId));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied to this session");
        }
        
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    private ChatSessionVO convertToVO(ChatSessionEntity entity) {
        return ChatSessionVO.builder()
                .sessionId(entity.getSessionId())
                .title(entity.getTitle())
                .relatedAppId(entity.getRelatedAppId())
                .createdAt(entity.getCreatedAt().toLocalDateTime())
                .updatedAt(entity.getUpdatedAt().toLocalDateTime())
                .build();
    }
}
