package com.example.lolserver.gamedata.adapter.out.persistence.mapper;

import com.example.lolserver.gamedata.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.gamedata.adapter.out.persistence.entity.SeasonEntity;
import com.example.lolserver.gamedata.adapter.out.persistence.entity.VersionEntity;
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
