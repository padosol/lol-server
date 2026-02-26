package com.example.lolserver.repository.season.mapper;

import com.example.lolserver.domain.season.application.model.SeasonReadModel;
import com.example.lolserver.repository.season.entity.SeasonEntity;
import com.example.lolserver.repository.version.entity.VersionEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeasonMapper {

    default SeasonReadModel toReadModel(SeasonEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SeasonReadModel(
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
