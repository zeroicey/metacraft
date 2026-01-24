package com.metacraft.api.modules.ai.repository;

import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import com.metacraft.api.modules.ai.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    // 以后做上下文记忆时会用到：查找某个会话最近的消息
    // List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}