package com.example.lolserver.community.adapter.out.persistence.mapper;

import com.example.lolserver.community.domain.Comment;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCommentEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommunityCommentMapper {

    Comment toDomain(CommunityCommentEntity entity);

    CommunityCommentEntity toEntity(Comment comment);

    List<Comment> toDomainList(List<CommunityCommentEntity> entities);
}
