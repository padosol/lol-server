package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.domain.QueueInfo;

import java.util.List;

public interface QueueTypePersistencePort {
    List<QueueInfo> findAll();
    List<QueueInfo> findAllByIsTabTrue();
}
