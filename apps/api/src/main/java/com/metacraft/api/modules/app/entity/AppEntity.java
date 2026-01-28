package com.metacraft.api.modules.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "apps")
public class AppEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
