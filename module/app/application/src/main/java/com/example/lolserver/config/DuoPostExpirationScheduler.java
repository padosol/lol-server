package com.example.lolserver.config;

import com.example.lolserver.duo.application.port.in.DuoPostExpirationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DuoPostExpirationScheduler {

    private static final long EXPIRATION_DELAY_MILLIS = 60_000L;

    private final DuoPostExpirationUseCase duoPostExpirationUseCase;

    @Scheduled(fixedDelay = EXPIRATION_DELAY_MILLIS)
    public void expireOverdueDuoPosts() {
        duoPostExpirationUseCase.expireOverduePosts();
    }
}
