package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.entity.GenerationTaskEntity;
import com.metacraft.api.modules.ai.repository.GenerationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GenerationTaskService {

    private final GenerationTaskRepository generationTaskRepository;

    @Transactional
    public void markRunning(String runId, String taskType) {
        GenerationTaskEntity task = generationTaskRepository.findByRunIdAndTaskType(runId, taskType)
                .orElseGet(() -> GenerationTaskEntity.builder()
                        .runId(runId)
                        .taskType(taskType)
                        .status("PENDING")
                        .attempt(0)
                        .build());

        task.setStatus("RUNNING");
        task.setAttempt((task.getAttempt() == null ? 0 : task.getAttempt()) + 1);
        task.setStartedAt(OffsetDateTime.now());
        task.setEndedAt(null);
        task.setErrorMessage(null);
        generationTaskRepository.save(task);
    }

    @Transactional
    public boolean tryMarkRunning(String runId, String taskType) {
        GenerationTaskEntity existing = generationTaskRepository.findByRunIdAndTaskType(runId, taskType).orElse(null);
        if (existing != null) {
            String status = existing.getStatus() == null ? "" : existing.getStatus().toUpperCase(Locale.ROOT);
            if ("RUNNING".equals(status) || "SUCCESS".equals(status)) {
                return false;
            }
        }
        markRunning(runId, taskType);
        return true;
    }

    @Transactional
    public void markSuccess(String runId, String taskType) {
        GenerationTaskEntity task = generationTaskRepository.findByRunIdAndTaskType(runId, taskType)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + runId + "/" + taskType));
        task.setStatus("SUCCESS");
        task.setEndedAt(OffsetDateTime.now());
        generationTaskRepository.save(task);
    }

    @Transactional
    public void markFailed(String runId, String taskType, String errorMessage) {
        GenerationTaskEntity task = generationTaskRepository.findByRunIdAndTaskType(runId, taskType)
                .orElseGet(() -> GenerationTaskEntity.builder()
                        .runId(runId)
                        .taskType(taskType)
                        .status("PENDING")
                        .attempt(0)
                        .build());
        task.setStatus("FAILED");
        task.setErrorMessage(errorMessage);
        task.setEndedAt(OffsetDateTime.now());
        generationTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Optional<String> getStatus(String runId, String taskType) {
        return generationTaskRepository.findByRunIdAndTaskType(runId, taskType)
                .map(GenerationTaskEntity::getStatus);
    }
}
