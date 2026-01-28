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
@Table(name = "app_versions")
public class AppVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "change_log", columnDefinition = "TEXT")
    private String changeLog;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
