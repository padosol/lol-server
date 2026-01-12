package com.example.lolserver.domain.queue_type.application.port.out;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;

import java.util.List;

public interface QueueTypePersistencePort {
    List<QueueInfo> findAll();
    List<QueueInfo> findAllByIsTabTrue();
}
