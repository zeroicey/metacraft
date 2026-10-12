package com.metacraft.api.modules.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.metacraft.api.modules.app.dto.CommentRequestDTO;
import com.metacraft.api.modules.app.dto.RatingRequestDTO;
import com.metacraft.api.modules.app.entity.AppCommentEntity;
import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.repository.AppCommentRepository;
import com.metacraft.api.modules.app.repository.AppRatingRepository;
import com.metacraft.api.modules.app.repository.AppRepository;
import com.metacraft.api.modules.app.vo.CommentVO;
import com.metacraft.api.modules.app.vo.RatingResultVO;
import com.metacraft.api.modules.user.entity.UserEntity;
import com.metacraft.api.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private AppRepository appRepository;

    @Mock
    private AppRatingRepository ratingRepository;

    @Mock
    private AppCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    void rateAppReturnsPersistedRatingSummary() {
        Long appId = 10L;
        Long userId = 20L;
        AppEntity app = AppEntity.builder().id(appId).isPublic(true).build();
        RatingRequestDTO request = new RatingRequestDTO();
        request.setRating(4);

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.empty());
        when(ratingRepository.findAverageRatingByAppId(appId)).thenReturn(4.0);
        when(ratingRepository.countByAppId(appId)).thenReturn(3L);

        RatingResultVO result = storeService.rateApp(appId, userId, request);

        assertThat(result.getAverageRating()).isEqualTo(4.0);
        assertThat(result.getRatingCount()).isEqualTo(3L);
        verify(ratingRepository).save(any());
    }

    @Test
    void addCommentReturnsTheSavedCommentWithAuthorDetails() {
        Long appId = 10L;
        Long userId = 20L;
        OffsetDateTime createdAt = OffsetDateTime.now();
        AppEntity app = AppEntity.builder().id(appId).isPublic(true).build();
        CommentRequestDTO request = new CommentRequestDTO();
        request.setContent("这个应用很好用");
        AppCommentEntity savedComment = AppCommentEntity.builder()
                .id(30L)
                .appId(appId)
                .userId(userId)
                .content(request.getContent())
                .createdAt(createdAt)
                .build();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("测试用户");

        when(appRepository.findById(appId)).thenReturn(Optional.of(app));
        when(commentRepository.save(any(AppCommentEntity.class))).thenReturn(savedComment);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CommentVO result = storeService.addComment(appId, userId, request);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getUserName()).isEqualTo("测试用户");
        assertThat(result.getContent()).isEqualTo("这个应用很好用");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }
}
