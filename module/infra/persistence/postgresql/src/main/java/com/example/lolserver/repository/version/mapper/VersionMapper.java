package com.example.lolserver.repository.version.mapper;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.repository.version.entity.VersionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface VersionMapper {

    VersionMapper INSTANCE = Mappers.getMapper(VersionMapper.class);

    default VersionReadModel entityToReadModel(VersionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new VersionReadModel(
                entity.getVersionId(),
                entity.getVersionValue(),
                entity.getCreatedAt()
        );
    }
}
