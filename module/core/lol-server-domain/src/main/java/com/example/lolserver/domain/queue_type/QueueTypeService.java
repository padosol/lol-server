package com.example.lolserver.domain.queue_type;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.repository.queue_type.QueueTypeRepository;
import com.example.lolserver.repository.queue_type.entity.QueueEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueTypeService {
    private final QueueTypeRepository queueTypeRepository;

    public List<QueueInfo> findAllByIsTabTrue() {
        List<QueueEntity> queueEntities = queueTypeRepository.findAllByIsTab(true);

        return queueEntities.stream().map(QueueInfo::new).toList();
    }
}
