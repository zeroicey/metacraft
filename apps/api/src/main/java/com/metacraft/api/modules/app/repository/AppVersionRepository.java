package com.metacraft.api.modules.app.repository;

import com.metacraft.api.modules.app.entity.AppVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersionEntity, Long> {
    List<AppVersionEntity> findByAppIdOrderByVersionNumberDesc(Long appId);
    
    Optional<AppVersionEntity> findByAppIdAndVersionNumber(Long appId, Integer versionNumber);
    
    Optional<AppVersionEntity> findTopByAppIdOrderByVersionNumberDesc(Long appId);
}
