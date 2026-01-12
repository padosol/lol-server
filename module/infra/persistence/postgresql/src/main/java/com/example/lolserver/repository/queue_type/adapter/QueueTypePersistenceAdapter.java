package com.example.lolserver.repository.queue_type.adapter;

import com.example.lolserver.domain.queue_type.application.port.out.QueueTypePersistencePort;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.repository.queue_type.QueueTypeRepository;
import com.example.lolserver.repository.queue_type.mapper.QueueTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QueueTypePersistenceAdapter implements QueueTypePersistencePort {

    private final QueueTypeRepository queueTypeRepository;
    private final QueueTypeMapper queueTypeMapper;

    @Override
    public List<QueueInfo> findAll() {
        return queueTypeRepository.findAll().stream()
                .map(queueTypeMapper::entityToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<QueueInfo> findAllByIsTabTrue() {
        return queueTypeRepository.findAllByIsTab(true).stream()
                .map(queueTypeMapper::entityToDomain)
                .collect(Collectors.toList());
    }
}
