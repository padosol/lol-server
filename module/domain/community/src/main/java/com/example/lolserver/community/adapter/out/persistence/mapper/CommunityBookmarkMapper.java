package com.example.lolserver.community.adapter.out.persistence.mapper;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import com.example.lolserver.community.domain.Bookmark;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommunityBookmarkMapper {

    Bookmark toDomain(CommunityBookmarkEntity entity);

    CommunityBookmarkEntity toEntity(Bookmark bookmark);
}
