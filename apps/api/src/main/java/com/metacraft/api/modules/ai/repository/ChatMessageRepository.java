package com.metacraft.api.modules.ai.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.metacraft.api.modules.ai.entity.ChatMessageEntity;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
    void deleteBySessionId(String sessionId);
}
