package com.metacraft.api.modules.ai.service;

import com.metacraft.api.modules.ai.entity.GenerationArtifactEntity;
import com.metacraft.api.modules.ai.repository.GenerationArtifactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationArtifactService {

    private final GenerationArtifactRepository generationArtifactRepository;

    @Transactional
    public void saveArtifact(String runId, String artifactType, Long refId, Map<String, Object> content) {
        try {
            GenerationArtifactEntity artifact = GenerationArtifactEntity.builder()
                    .runId(runId)
                    .artifactType(artifactType)
                    .refId(refId)
                    .contentJson(content)
                    .build();
            generationArtifactRepository.save(artifact);
        } catch (Exception e) {
            log.error("Failed to save artifact runId={}, type={}", runId, artifactType, e);
            throw new IllegalStateException("Failed to save generation artifact", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getLatestArtifactContent(String runId, String artifactType) {
        return generationArtifactRepository.findFirstByRunIdAndArtifactTypeOrderByIdDesc(runId, artifactType)
                .flatMap(artifact -> Optional.ofNullable(artifact.getContentJson()));
    }
}
