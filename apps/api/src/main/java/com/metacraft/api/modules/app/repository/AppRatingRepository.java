package com.metacraft.api.modules.app.repository;

import com.metacraft.api.modules.app.entity.AppRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppRatingRepository extends JpaRepository<AppRatingEntity, Long> {

    Optional<AppRatingEntity> findByAppIdAndUserId(Long appId, Long userId);

    @Query("SELECT AVG(r.rating) FROM AppRatingEntity r WHERE r.appId = :appId")
    Double findAverageRatingByAppId(@Param("appId") Long appId);

    long countByAppId(Long appId);
}