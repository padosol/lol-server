package com.example.lolserver.gamedata.adapter.out.persistence.mapper;

import com.example.lolserver.gamedata.domain.QueueInfo;
import com.example.lolserver.gamedata.adapter.out.persistence.entity.QueueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface QueueTypeMapper {

    QueueTypeMapper INSTANCE = Mappers.getMapper(QueueTypeMapper.class);

    default QueueInfo entityToDomain(QueueEntity queueEntity) {
        return new QueueInfo(
                queueEntity.getQueueId(),
                queueEntity.getQueueName(),
                queueEntity.isTab()
        );
    }
}
