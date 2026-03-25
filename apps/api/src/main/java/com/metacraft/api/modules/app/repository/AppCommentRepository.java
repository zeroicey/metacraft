package com.metacraft.api.modules.app.repository;

import com.metacraft.api.modules.app.entity.AppCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppCommentRepository extends JpaRepository<AppCommentEntity, Long> {

    @Query("SELECT c FROM AppCommentEntity c WHERE c.appId = :appId ORDER BY c.createdAt DESC")
    List<AppCommentEntity> findByAppIdOrderByCreatedAtDesc(Long appId);
}