package com.metacraft.api.modules.app.assembler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.metacraft.api.modules.app.entity.AppEntity;
import com.metacraft.api.modules.app.entity.AppVersionEntity;
import com.metacraft.api.modules.app.vo.AppVO;
import com.metacraft.api.modules.app.vo.AppVersionVO;

@Component
public class AppAssembler {

    public AppVO toAppVO(AppEntity entity, List<AppVersionEntity> versions) {
        return AppVO.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
                .logo(entity.getLogo())
                .isPublic(entity.getIsPublic())
                .currentVersionId(entity.getCurrentVersionId())
                .versions(toVersionVOs(versions))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<AppVersionVO> toVersionVOs(List<AppVersionEntity> versions) {
        if (versions == null || versions.isEmpty()) {
            return Collections.emptyList();
        }

        return versions.stream()
                .map(this::toVersionVO)
                .toList();
    }

    public AppVersionVO toVersionVO(AppVersionEntity entity) {
        return AppVersionVO.builder()
                .id(entity.getId())
                .versionNumber(entity.getVersionNumber())
                .storagePath(entity.getStoragePath())
                .changeLog(entity.getChangeLog())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}