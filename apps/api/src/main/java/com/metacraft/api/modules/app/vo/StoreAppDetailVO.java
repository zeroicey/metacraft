package com.metacraft.api.modules.app.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreAppDetailVO extends StoreAppListVO {
    private List<CommentVO> comments;
}