package com.metacraft.api.modules.ai.repository;

import com.metacraft.api.modules.ai.entity.GenerationArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationArtifactRepository extends JpaRepository<GenerationArtifactEntity, Long> {
    List<GenerationArtifactEntity> findByRunIdOrderByCreatedAtAsc(String runId);
    Optional<GenerationArtifactEntity> findFirstByRunIdAndArtifactTypeOrderByIdDesc(String runId, String artifactType);
}
