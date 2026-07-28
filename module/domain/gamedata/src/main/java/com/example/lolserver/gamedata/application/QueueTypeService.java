package com.example.lolserver.gamedata.application;

import com.example.lolserver.gamedata.application.port.in.QueueTypeUseCase;
import com.example.lolserver.gamedata.application.port.out.QueueTypePersistencePort;
import com.example.lolserver.gamedata.domain.QueueInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
