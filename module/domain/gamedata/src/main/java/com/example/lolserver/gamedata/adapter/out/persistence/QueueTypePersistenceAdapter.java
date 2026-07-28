package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.application.port.out.QueueTypePersistencePort;
import com.example.lolserver.gamedata.domain.QueueInfo;
import com.example.lolserver.gamedata.adapter.out.persistence.mapper.QueueTypeMapper;
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
