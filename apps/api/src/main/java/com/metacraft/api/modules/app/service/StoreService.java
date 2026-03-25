package com.metacraft.api.modules.app.service;

import com.metacraft.api.exception.UnauthorizedException;
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
import com.metacraft.api.modules.app.vo.StoreAppDetailVO;
import com.metacraft.api.modules.app.vo.StoreAppListVO;
import com.metacraft.api.modules.user.entity.UserEntity;
import com.metacraft.api.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final AppRepository appRepository;
    private final AppRatingRepository ratingRepository;
    private final AppCommentRepository commentRepository;
    private final UserRepository userRepository;

    // Simple in-memory cache for rating stats: appId -> (averageRating, ratingCount)
    private final Map<Long, RatingCache> ratingCache = new ConcurrentHashMap<>();

    private static class RatingCache {
        double averageRating;
        long ratingCount;

        RatingCache(double averageRating, long ratingCount) {
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
        }
    }

    @Transactional(readOnly = true)
    public List<StoreAppListVO> getPublishedApps() {
        List<AppEntity> apps = appRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        return apps.stream()
                .map(this::toStoreAppListVO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreAppDetailVO getAppDetail(Long appId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new UnauthorizedException("App not found"));

        if (!Boolean.TRUE.equals(app.getIsPublic())) {
            throw new UnauthorizedException("App is not published");
        }

        StoreAppDetailVO detailVO = StoreAppDetailVO.builder()
                .id(app.getId())
                .uuid(app.getUuid())
                .name(app.getName())
                .description(app.getDescription())
                .logo(app.getLogo())
                .author(getAuthorVO(app.getUserId()))
                .createdAt(app.getCreatedAt())
                .build();

        // Get rating info
        RatingCache cache = ratingCache.get(appId);
        if (cache != null) {
            detailVO.setAverageRating(cache.averageRating);
            detailVO.setRatingCount(cache.ratingCount);
        } else {
            Double avg = ratingRepository.findAverageRatingByAppId(appId);
            long count = ratingRepository.countByAppId(appId);
            detailVO.setAverageRating(avg != null ? avg : 0.0);
            detailVO.setRatingCount(count);
        }

        // Get comments
        List<CommentVO> comments = commentRepository.findByAppIdOrderByCreatedAtDesc(appId)
                .stream()
                .map(this::toCommentVO)
                .collect(Collectors.toList());
        detailVO.setComments(comments);

        return detailVO;
    }

    @Transactional
    public void publishApp(Long appId, Long userId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new UnauthorizedException("App not found"));

        if (!app.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only publish your own apps");
        }

        app.setIsPublic(true);
        appRepository.save(app);
    }

    @Transactional
    public void unpublishApp(Long appId, Long userId) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new UnauthorizedException("App not found"));

        if (!app.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only unpublish your own apps");
        }

        app.setIsPublic(false);
        appRepository.save(app);
    }

    @Transactional
    public void rateApp(Long appId, Long userId, RatingRequestDTO request) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new UnauthorizedException("App not found"));

        if (!Boolean.TRUE.equals(app.getIsPublic())) {
            throw new UnauthorizedException("App is not published");
        }

        Optional<AppRatingEntity> existingRating = ratingRepository.findByAppIdAndUserId(appId, userId);

        AppRatingEntity rating;
        if (existingRating.isPresent()) {
            rating = existingRating.get();
            rating.setRating(request.getRating());
        } else {
            rating = AppRatingEntity.builder()
                    .appId(appId)
                    .userId(userId)
                    .rating(request.getRating())
                    .build();
        }

        ratingRepository.save(rating);
        updateAppRatingCache(appId);
    }

    @Transactional
    public void addComment(Long appId, Long userId, CommentRequestDTO request) {
        AppEntity app = appRepository.findById(appId)
                .orElseThrow(() -> new UnauthorizedException("App not found"));

        if (!Boolean.TRUE.equals(app.getIsPublic())) {
            throw new UnauthorizedException("App is not published");
        }

        AppCommentEntity comment = AppCommentEntity.builder()
                .appId(appId)
                .userId(userId)
                .content(request.getContent())
                .build();

        commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        AppCommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new UnauthorizedException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    private StoreAppListVO toStoreAppListVO(AppEntity app) {
        StoreAppListVO vo = StoreAppListVO.builder()
                .id(app.getId())
                .uuid(app.getUuid())
                .name(app.getName())
                .description(app.getDescription())
                .logo(app.getLogo())
                .author(getAuthorVO(app.getUserId()))
                .createdAt(app.getCreatedAt())
                .build();

        // Get rating info
        RatingCache cache = ratingCache.get(app.getId());
        if (cache != null) {
            vo.setAverageRating(cache.averageRating);
            vo.setRatingCount(cache.ratingCount);
        } else {
            Double avg = ratingRepository.findAverageRatingByAppId(app.getId());
            long count = ratingRepository.countByAppId(app.getId());
            vo.setAverageRating(avg != null ? avg : 0.0);
            vo.setRatingCount(count);
        }

        return vo;
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
        UserEntity user = userRepository.findById(comment.getUserId()).orElse(null);
        return CommentVO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .userName(user != null ? user.getName() : "Unknown")
                .userAvatar(user != null ? user.getAvatarBase64() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private void updateAppRatingCache(Long appId) {
        Double avg = ratingRepository.findAverageRatingByAppId(appId);
        long count = ratingRepository.countByAppId(appId);
        ratingCache.put(appId, new RatingCache(avg != null ? avg : 0.0, count));
    }
}