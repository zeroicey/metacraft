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
    private Long ratingCount;
}