package com.example.lolserver.repository.duo.mapper;

import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.repository.duo.entity.DuoRequestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", imports = {Lane.class, DuoRequestStatus.class})
public interface DuoRequestMapper {

    @Mapping(target = "primaryLane", expression = "java(Lane.valueOf(entity.getPrimaryLane()))")
    @Mapping(target = "secondaryLane", expression = "java(Lane.valueOf(entity.getSecondaryLane()))")
    @Mapping(target = "status", expression = "java(DuoRequestStatus.valueOf(entity.getStatus()))")
    @Mapping(source = "tierRank", target = "rank")
    DuoRequest toDomain(DuoRequestEntity entity);

    @Mapping(target = "primaryLane", expression = "java(request.getPrimaryLane().name())")
    @Mapping(target = "secondaryLane", expression = "java(request.getSecondaryLane().name())")
    @Mapping(target = "status", expression = "java(request.getStatus().name())")
    @Mapping(source = "rank", target = "tierRank")
    DuoRequestEntity toEntity(DuoRequest request);

    List<DuoRequest> toDomainList(List<DuoRequestEntity> entities);
}
