package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.domain.QueueInfo;

import java.util.List;

public interface QueueTypeUseCase {
    List<QueueInfo> getQueueInfo();
    List<QueueInfo> findAllByIsTabTrue();
}
