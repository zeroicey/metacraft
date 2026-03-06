package com.metacraft.api.modules.ai.repository;

import com.metacraft.api.modules.ai.entity.GenerationTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTaskEntity, Long> {
    Optional<GenerationTaskEntity> findByRunIdAndTaskType(String runId, String taskType);
    List<GenerationTaskEntity> findByRunIdOrderByCreatedAtAsc(String runId);
}
