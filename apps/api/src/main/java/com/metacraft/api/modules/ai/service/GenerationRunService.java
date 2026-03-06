package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.entity.GenerationRunEntity;
import com.metacraft.api.modules.ai.repository.GenerationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class GenerationRunService {

    private final GenerationRunRepository generationRunRepository;

    @Transactional
    public GenerationRunEntity createRun(String runId, Long userId, String sessionId) {
        GenerationRunEntity run = GenerationRunEntity.builder()
                .runId(runId)
                .userId(userId)
                .sessionId(sessionId)
                .status("CREATED")
                .build();
        return generationRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public GenerationRunEntity getRun(String runId, Long userId) {
        return generationRunRepository.findByRunIdAndUserId(runId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
    }

    @Transactional
    public void markIntentDone(String runId, String intent) {
        GenerationRunEntity run = generationRunRepository.findByRunId(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.setIntent(intent);
        run.setStatus("INTENT_DONE");
        generationRunRepository.save(run);
    }

    @Transactional
    public void markSucceeded(String runId, String sessionId) {
        GenerationRunEntity run = generationRunRepository.findByRunId(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.setStatus("SUCCEEDED");
        run.setSessionId(sessionId);
        run.setCompletedAt(OffsetDateTime.now());
        generationRunRepository.save(run);
    }

    @Transactional
    public void markFailed(String runId, String errorMessage) {
        GenerationRunEntity run = generationRunRepository.findByRunId(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.setStatus("FAILED");
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(OffsetDateTime.now());
        generationRunRepository.save(run);
    }

    @Transactional
    public void cancelRun(String runId, Long userId) {
        GenerationRunEntity run = generationRunRepository.findByRunIdAndUserId(runId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.setStatus("CANCELLED");
        run.setCompletedAt(OffsetDateTime.now());
        generationRunRepository.save(run);
    }
}
