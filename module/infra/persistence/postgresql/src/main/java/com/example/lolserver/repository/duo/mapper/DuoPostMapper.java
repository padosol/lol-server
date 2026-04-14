package com.example.lolserver.repository.duo.mapper;

import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.repository.duo.entity.DuoPostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = {Lane.class, DuoPostStatus.class})
public interface DuoPostMapper {

    @Mapping(target = "primaryLane", expression = "java(Lane.from(entity.getPrimaryLane()))")
    @Mapping(target = "secondaryLane", expression = "java(Lane.from(entity.getSecondaryLane()))")
    @Mapping(target = "status", expression = "java(DuoPostStatus.valueOf(entity.getStatus()))")
    @Mapping(source = "tierRank", target = "rank")
    DuoPost toDomain(DuoPostEntity entity);

    @Mapping(target = "primaryLane", expression = "java(post.getPrimaryLane().name())")
    @Mapping(target = "secondaryLane", expression = "java(post.getSecondaryLane().name())")
    @Mapping(target = "status", expression = "java(post.getStatus().name())")
    @Mapping(source = "rank", target = "tierRank")
    DuoPostEntity toEntity(DuoPost post);

    List<DuoPost> toDomainList(List<DuoPostEntity> entities);
}
