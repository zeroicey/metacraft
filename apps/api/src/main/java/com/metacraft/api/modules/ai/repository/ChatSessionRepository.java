package com.metacraft.api.modules.ai.repository;

import com.metacraft.api.modules.ai.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {
    Optional<ChatSessionEntity> findBySessionId(String sessionId);
    List<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);
    void deleteBySessionId(String sessionId);
}
