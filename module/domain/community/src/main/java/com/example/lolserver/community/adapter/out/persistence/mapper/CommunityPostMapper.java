package com.example.lolserver.community.adapter.out.persistence.mapper;

import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommunityPostMapper {

    Post toDomain(CommunityPostEntity entity);

    CommunityPostEntity toEntity(Post post);
}
