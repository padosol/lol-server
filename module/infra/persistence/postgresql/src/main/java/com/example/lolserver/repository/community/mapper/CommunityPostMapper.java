package com.example.lolserver.repository.community.mapper;

import com.example.lolserver.domain.community.domain.Post;
import com.example.lolserver.repository.community.entity.CommunityPostEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommunityPostMapper {

    Post toDomain(CommunityPostEntity entity);

    CommunityPostEntity toEntity(Post post);
}
