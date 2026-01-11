package com.example.lolserver.domain.queue_type.application.port.in;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;

import java.util.List;

public interface QueueTypeUseCase {
    List<QueueInfo> getQueueInfo();
    List<QueueInfo> findAllByIsTabTrue();
}
