package com.example.lolserver.repository.queue_type.mapper;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.repository.queue_type.entity.QueueEntity;
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
