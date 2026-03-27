# 应用商店与发布功能实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MetaCraft 添加应用商店功能，支持用户发布应用、评分、评论

**Architecture:** 新增评分和评论两张表，复用现有的 AppEntity.isPublic 字段，通过 StoreController 暴露 REST API

**Tech Stack:** Java 21, Spring Boot 3.5.9, Spring Data JPA, Flyway

---

## 文件结构

```
apps/api/src/main/java/com/metacraft/api/modules/app/
├── controller/
│   └── StoreController.java          # 新增：商店接口
├── entity/
│   ├── AppRatingEntity.java           # 新增：评分实体
│   └── AppCommentEntity.java         # 新增：评论实体
├── repository/
│   ├── AppRatingRepository.java      # 新增：评分仓库
│   └── AppCommentRepository.java     # 新增：评论仓库
├── service/
│   └── StoreService.java             # 新增：商店服务（整合评分/评论）
├── dto/
│   ├── RatingRequestDTO.java         # 新增：评分请求
│   └── CommentRequestDTO.java        # 新增：评论请求
└── vo/
    ├── StoreAppListVO.java           # 新增：商店应用列表VO
    └── StoreAppDetailVO.java         # 新增：商店应用详情VO

apps/api/src/main/resources/db/migration/
└── V8__add_store_tables.sql          # 新增：数据库迁移
```

---

## Chunk 1: 数据库迁移

### Task 1: 创建数据库迁移文件

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V8__add_store_tables.sql`

- [ ] **Step 1: 创建迁移文件**

```sql
-- 新增字段到 apps 表
ALTER TABLE apps
ADD COLUMN average_rating DOUBLE PRECISION,
ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN apps.average_rating IS 'Average rating (cached)';
COMMENT ON COLUMN apps.rating_count IS 'Number of ratings';

-- 创建 app_ratings 表
CREATE TABLE app_ratings (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (app_id, user_id)
);

CREATE INDEX idx_app_ratings_app_id ON app_ratings(app_id);

COMMENT ON TABLE app_ratings IS 'App ratings table';
COMMENT ON COLUMN app_ratings.app_id IS 'App ID';
COMMENT ON COLUMN app_ratings.user_id IS 'User who rated';
COMMENT ON COLUMN app_ratings.rating IS 'Rating 1-5';

-- 创建 app_comments 表
CREATE TABLE app_comments (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_comments_app_id ON app_comments(app_id);

COMMENT ON TABLE app_comments IS 'App comments table';
COMMENT ON COLUMN app_comments.app_id IS 'App ID';
COMMENT ON COLUMN app_comments.user_id IS 'Comment author';
COMMENT ON COLUMN app_comments.content IS 'Comment content';
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/resources/db/migration/V8__add_store_tables.sql
git commit -m "feat(api): add store database migration V8"
```

---

## Chunk 2: 实体类

### Task 2: 创建 AppRatingEntity

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/entity/AppRatingEntity.java`

- [ ] **Step 1: 创建评分实体**

```java
package com.metacraft.api.modules.app.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_ratings")
public class AppRatingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/entity/AppRatingEntity.java
git commit -m "feat(api): add AppRatingEntity"
```

### Task 3: 创建 AppCommentEntity

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/entity/AppCommentEntity.java`

- [ ] **Step 1: 创建评论实体**

```java
package com.metacraft.api.modules.app.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_comments")
public class AppCommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/entity/AppCommentEntity.java
git commit -m "feat(api): add AppCommentEntity"
```

---

## Chunk 3: Repository

### Task 4: 创建 AppRatingRepository

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/repository/AppRatingRepository.java`

- [ ] **Step 1: 创建评分仓库**

```java
package com.metacraft.api.modules.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.metacraft.api.modules.app.entity.AppRatingEntity;

@Repository
public interface AppRatingRepository extends JpaRepository<AppRatingEntity, Long> {
    Optional<AppRatingEntity> findByAppIdAndUserId(Long appId, Long userId);

    @Query("SELECT AVG(r.rating) FROM AppRatingEntity r WHERE r.appId = :appId")
    Double findAverageRatingByAppId(Long appId);

    @Query("SELECT COUNT(r) FROM AppRatingEntity r WHERE r.appId = :appId")
    Integer countByAppId(Long appId);
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/repository/AppRatingRepository.java
git commit -m "feat(api): add AppRatingRepository"
```

### Task 5: 创建 AppCommentRepository

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/repository/AppCommentRepository.java`

- [ ] **Step 1: 创建评论仓库**

```java
package com.metacraft.api.modules.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.metacraft.api.modules.app.entity.AppCommentEntity;

@Repository
public interface AppCommentRepository extends JpaRepository<AppCommentEntity, Long> {

    @Query("SELECT c FROM AppCommentEntity c WHERE c.appId = :appId ORDER BY c.createdAt DESC")
    List<AppCommentEntity> findByAppIdOrderByCreatedAtDesc(Long appId);
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/repository/AppCommentRepository.java
git commit -m "feat(api): add AppCommentRepository"
```

---

## Chunk 4: DTO

### Task 6: 创建请求 DTO

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/dto/RatingRequestDTO.java`
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/dto/CommentRequestDTO.java`

- [ ] **Step 1: 创建 RatingRequestDTO**

```java
package com.metacraft.api.modules.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingRequestDTO {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;
}
```

- [ ] **Step 2: 创建 CommentRequestDTO**

```java
package com.metacraft.api.modules.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDTO {
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;
}
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/dto/RatingRequestDTO.java
git add apps/api/src/main/java/com/metacraft/api/modules/app/dto/CommentRequestDTO.java
git commit -m "feat(api): add RatingRequestDTO and CommentRequestDTO"
```

---

## Chunk 5: VO

### Task 7: 创建 VO 类

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/vo/StoreAppListVO.java`
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/vo/StoreAppDetailVO.java`
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/vo/AuthorVO.java`
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/vo/CommentVO.java`
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/vo/RatingResultVO.java`

- [ ] **Step 1: 创建所有 VO 类**

**StoreAppListVO.java:**
```java
package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAppListVO {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String logo;
    private Double averageRating;
    private Integer ratingCount;
    private AuthorVO author;
    private String createdAt;
}
```

**AuthorVO.java:**
```java
package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorVO {
    private Long id;
    private String name;
    private String avatarBase64;
}
```

**CommentVO.java:**
```java
package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private String createdAt;
}
```

**StoreAppDetailVO.java:**
```java
package com.metacraft.api.modules.app.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAppDetailVO {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String logo;
    private Double averageRating;
    private Integer ratingCount;
    private AuthorVO author;
    private String createdAt;
    private List<CommentVO> comments;
}
```

**RatingResultVO.java:**
```java
package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResultVO {
    private Double averageRating;
    private Integer ratingCount;
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/vo/StoreAppListVO.java
git add apps/api/src/main/java/com/metacraft/api/modules/app/vo/StoreAppDetailVO.java
git add apps/api/src/main/java/com/metacraft/api/modules/app/vo/AuthorVO.java
git add apps/api/src/main/java/com/metacraft/api/modules/app/vo/CommentVO.java
git add apps/api/src/main/java/com/metacraft/api/modules/app/vo/RatingResultVO.java
git commit -m "feat(api): add store VO classes"
```

---

## Chunk 6: Service

### Task 8: 创建 StoreService

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/service/StoreService.java`
- Modify: `apps/api/src/main/java/com/metacraft/api/modules/app/repository/AppRepository.java`

- [ ] **Step 1: 修改 AppRepository 添加查询方法**

在 AppRepository.java 添加:
```java
List<AppEntity> findByIsPublicTrueOrderByCreatedAtDesc();
```

- [ ] **Step 2: 创建 StoreService**

```java
package com.metacraft.api.modules.app.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.metacraft.api.modules.app.dto.CommentRequestDTO;
import com.metacraft.api.modules.app.dto.RatingRequestDTO;
import com.metacraft.api.modules.app.entity.AppCommentEntity;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppRatingEntity;
import com.metacraft.api.modules.app.repository.AppCommentRepository;
import com.metacraft.api.modules.app.repository.AppRatingRepository;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.vo.AuthorVO;
import com.metacraft.api.modules.app.vo.CommentVO;
import com.metacraft.api.modules.app.vo.RatingResultVO;
import com.metacraft.api.modules.app.vo.StoreAppDetailVO;
import com.metacraft.api.modules.app.vo.StoreAppListVO;
import com.metacraft.api.modules.user.entity.UserEntity;
import com.metacraft.api.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final AppRepository appRepository;
    private final AppRatingRepository appRatingRepository;
    private final AppCommentRepository appCommentRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // ===== 应用列表 =====

    public List<StoreAppListVO> getPublishedApps() {
        List<AppEntity> apps = appRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        return apps.stream().map(this::toStoreAppListVO).collect(Collectors.toList());
    }

    // ===== 应用详情 =====

    public StoreAppDetailVO getAppDetail(Long appId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("应用不存在"));

        if (!Boolean.TRUE.equals(app.getIsPublic())) {
            throw new RuntimeException("应用未发布");
        }

        List<CommentVO> comments = appCommentRepository.findByAppIdOrderByCreatedAtDesc(appId)
                .stream()
                .map(this::toCommentVO)
                .collect(Collectors.toList());

        return StoreAppDetailVO.builder()
                .id(app.getId())
                .uuid(app.getUuid())
                .name(app.getName())
                .description(app.getDescription())
                .logo(app.getLogo())
                .averageRating(app.getAverageRating())
                .ratingCount(app.getRatingCount())
                .author(getAuthorVO(app.getUserId()))
                .createdAt(app.getCreatedAt() != null ? app.getCreatedAt().format(ISO_FORMATTER) : null)
                .comments(comments)
                .build();
    }

    // ===== 发布/下架 =====

    @Transactional
    public void publishApp(Long appId, Long userId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("应用不存在"));

        if (!app.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作");
        }

        app.setIsPublic(true);
        appRepository.save(app);
    }

    @Transactional
    public void unpublishApp(Long appId, Long userId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("应用不存在"));

        if (!app.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作");
        }

        app.setIsPublic(false);
        appRepository.save(app);
    }

    // ===== 评分 =====

    @Transactional
    public RatingResultVO rateApp(Long appId, Long userId, RatingRequestDTO request) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("应用不存在"));

        if (!Boolean.TRUE.equals(app.getIsPublic())) {
            throw new RuntimeException("应用未发布，无法评分");
        }

        // 检查是否已评分
        AppRatingEntity existingRating = appRatingRepository.findByAppIdAndUserId(appId, userId)
                .orElse(null);

        if (existingRating != null) {
            // 更新评分
            existingRating.setRating(request.getRating());
            appRatingRepository.save(existingRating);
        } else {
            // 新增评分
            AppRatingEntity rating = AppRatingEntity.builder()
                    .appId(appId)
                    .userId(userId)
                    .rating(request.getRating())
                    .build();
            appRatingRepository.save(rating);
        }

        // 更新应用评分缓存
        updateAppRatingCache(appId);

        AppEntity updatedApp = appRepository.findById(appId).get();
        return RatingResultVO.builder()
                .averageRating(updatedApp.getAverageRating())
                .ratingCount(updatedApp.getRatingCount())
                .build();
    }

    private void updateAppRatingCache(Long appId) {
        Double avgRating = appRatingRepository.findAverageRatingByAppId(appId);
        Integer count = appRatingRepository.countByAppId(appId);

        AppEntity app = appRepository.findById(appId).get();
        app.setAverageRating(avgRating);
        app.setRatingCount(count);
        appRepository.save(app);
    }

    // ===== 评论 =====

    @Transactional
    public CommentVO addComment(Long appId, Long userId, CommentRequestDTO request) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("应用不存在"));

        if (!Boolean.TRUE.equals(app.getIsPublic())) {
            throw new RuntimeException("应用未发布，无法评论");
        }

        AppCommentEntity comment = AppCommentEntity.builder()
                .appId(appId)
                .userId(userId)
                .content(request.getContent())
                .build();

        comment = appCommentRepository.save(comment);
        return toCommentVO(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        AppCommentEntity comment = appCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此评论");
        }

        appCommentRepository.delete(comment);
    }

    // ===== 辅助方法 =====

    private StoreAppListVO toStoreAppListVO(AppEntity app) {
        return StoreAppListVO.builder()
                .id(app.getId())
                .uuid(app.getUuid())
                .name(app.getName())
                .description(app.getDescription())
                .logo(app.getLogo())
                .averageRating(app.getAverageRating())
                .ratingCount(app.getRatingCount())
                .author(getAuthorVO(app.getUserId()))
                .createdAt(app.getCreatedAt() != null ? app.getCreatedAt().format(ISO_FORMATTER) : null)
                .build();
    }

    private AuthorVO getAuthorVO(Long userId) {
        return userRepository.findById(userId)
                .map(user -> AuthorVO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .avatarBase64(user.getAvatarBase64())
                        .build())
                .orElse(null);
    }

    private CommentVO toCommentVO(AppCommentEntity comment) {
        String userName = "未知用户";
        String userAvatar = null;

        UserEntity user = userRepository.findById(comment.getUserId()).orElse(null);
        if (user != null) {
            userName = user.getName();
            userAvatar = user.getAvatarBase64();
        }

        return CommentVO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .userName(userName)
                .userAvatar(userAvatar)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().format(ISO_FORMATTER) : null)
                .build();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/service/StoreService.java
git add apps/api/src/main/java/com/metacraft/api/modules/app/repository/AppRepository.java
git commit -m "feat(api): add StoreService"
```

---

## Chunk 7: Controller

### Task 9: 创建 StoreController

**Files:**
- Create: `apps/api/src/main/java/com/metacraft/api/modules/app/controller/StoreController.java`

- [ ] **Step 1: 创建 StoreController**

```java
package com.metacraft.api.modules.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metacraft.api.modules.app.dto.CommentRequestDTO;
import com.metacraft.api.modules.app.dto.RatingRequestDTO;
import com.metacraft.api.modules.app.service.StoreService;
import com.metacraft.api.modules.app.vo.CommentVO;
import com.metacraft.api.modules.app.vo.RatingResultVO;
import com.metacraft.api.modules.app.vo.StoreAppDetailVO;
import com.metacraft.api.modules.app.vo.StoreAppListVO;
import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/store/apps")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // ===== 公开接口 =====

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreAppListVO>>> getStoreApps() {
        List<StoreAppListVO> apps = storeService.getPublishedApps();
        return ResponseEntity.ok(ApiResponse.ok("success", apps));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreAppDetailVO>> getAppDetail(@PathVariable Long id) {
        StoreAppDetailVO detail = storeService.getAppDetail(id);
        return ResponseEntity.ok(ApiResponse.ok("success", detail));
    }

    // ===== 需要认证的接口 =====

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> publishApp(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        storeService.publishApp(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("应用已发布", null));
    }

    @DeleteMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Void>> unpublishApp(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        storeService.unpublishApp(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("应用已下架", null));
    }

    @PostMapping("/{id}/ratings")
    public ResponseEntity<ApiResponse<RatingResultVO>> rateApp(
            @PathVariable Long id,
            @Valid @RequestBody RatingRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RatingResultVO result = storeService.rateApp(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("评分成功", result));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentVO>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CommentVO comment = storeService.addComment(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("评论成功", comment));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        storeService.deleteComment(commentId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("评论已删除", null));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/java/com/metacraft/api/modules/app/controller/StoreController.java
git commit -m "feat(api): add StoreController"
```

---

## Chunk 8: 异常处理

### Task 10: 调整异常处理

**Files:**
- Modify: `apps/api/src/main/java/com/metacraft/api/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 确保 RuntimeException 返回 400 状态码**

确认现有的 RuntimeException 处理返回 400 状态码:

```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
    return Response.error(ex.getMessage()).status(HttpStatus.BAD_REQUEST.value()).build();
}
```

- [ ] **Step 2: Commit（如果修改了）**

```bash
git add apps/api/src/main/java/com/metacraft/api/exception/GlobalExceptionHandler.java
git commit -m "feat(api): ensure RuntimeException returns 400 status"
```

---

## 完成

```bash
git add -A
git commit -m "feat(api): add app store functionality - publish, rate, and comment on apps"
```