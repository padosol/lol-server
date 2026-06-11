package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.port.in.DuoPostExpirationUseCase;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuoPostExpirationService implements DuoPostExpirationUseCase {

    private final DuoPostPersistencePort duoPostPersistencePort;
    private final DuoRequestPersistencePort duoRequestPersistencePort;

    @Override
    @Transactional
    public void expireOverduePosts() {
        List<Long> expiredPostIds = duoPostPersistencePort.expireAllOverdue(LocalDateTime.now());

        for (Long postId : expiredPostIds) {
            duoRequestPersistencePort.closeAllOpen(postId);
        }

        if (!expiredPostIds.isEmpty()) {
            log.info("Expired {} duo posts: {}", expiredPostIds.size(), expiredPostIds);
        }
    }
}
