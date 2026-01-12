package com.example.lolserver.domain.queue_type.application;

import com.example.lolserver.domain.queue_type.application.port.in.QueueTypeUseCase;
import com.example.lolserver.domain.queue_type.application.port.out.QueueTypePersistencePort;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueTypeService implements QueueTypeUseCase {

    private final QueueTypePersistencePort queueTypePersistencePort;

    @Override
    public List<QueueInfo> getQueueInfo() {
        return queueTypePersistencePort.findAll();
    }

    @Override
    public List<QueueInfo> findAllByIsTabTrue() {
        return queueTypePersistencePort.findAllByIsTabTrue();
    }
}
