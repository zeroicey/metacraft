package com.metacraft.api.modules.ai.repository;

import com.metacraft.api.modules.ai.entity.GenerationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, Long> {
    Optional<GenerationRunEntity> findByRunId(String runId);
    Optional<GenerationRunEntity> findByRunIdAndUserId(String runId, Long userId);
}
