package com.example.lolserver.repository.season.mapper;

import com.example.lolserver.domain.season.application.dto.SeasonResponse;
import com.example.lolserver.repository.season.entity.SeasonEntity;
import com.example.lolserver.repository.version.entity.VersionEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeasonMapper {

    default SeasonResponse toResponse(SeasonEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SeasonResponse(
                entity.getSeasonValue(),
                entity.getSeasonName(),
                entity.getPatchVersions() == null
                        ? List.of()
                        : entity.getPatchVersions().stream()
                                .map(VersionEntity::getVersionValue)
                                .toList()
        );
    }
}
