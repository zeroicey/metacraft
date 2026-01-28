package com.metacraft.api.modules.ai.entity;

import com.metacraft.api.modules.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_messages")
public class ChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String role; // "user" 或 "assistant"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50, nullable = false)
    @Builder.Default
    private String type = "text"; // "text", "app"

    @Column(name = "related_app_id")
    private Long relatedAppId;

    @Column(name = "related_version_id")
    private Long relatedVersionId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}