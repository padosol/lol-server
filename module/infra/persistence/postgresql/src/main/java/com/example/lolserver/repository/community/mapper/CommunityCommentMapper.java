package com.example.lolserver.repository.community.mapper;

import com.example.lolserver.domain.community.domain.Comment;
import com.example.lolserver.repository.community.entity.CommunityCommentEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommunityCommentMapper {

    Comment toDomain(CommunityCommentEntity entity);

    CommunityCommentEntity toEntity(Comment comment);

    List<Comment> toDomainList(List<CommunityCommentEntity> entities);
}
