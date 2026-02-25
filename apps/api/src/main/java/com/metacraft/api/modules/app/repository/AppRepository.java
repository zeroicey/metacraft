package com.metacraft.api.modules.app.repository;

import com.metacraft.api.modules.app.entity.AppEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppRepository extends JpaRepository<AppEntity, Long> {
    List<AppEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<AppEntity> findByUuid(String uuid);
    Optional<AppEntity> findTopByUserIdOrderByIdDesc(Long userId);
}
