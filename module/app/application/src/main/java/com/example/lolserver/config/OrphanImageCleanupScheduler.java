package com.example.lolserver.config;

import com.example.lolserver.community.application.port.in.ImageCleanupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 컴포지션 루트에는 얇은 트리거만 두고 실제 로직은 도메인 UseCase 가 갖는다
 * ({@link DuoPostExpirationScheduler} 와 같은 패턴).
 */
@Component
@RequiredArgsConstructor
public class OrphanImageCleanupScheduler {

    private final ImageCleanupUseCase imageCleanupUseCase;

    /** 트래픽이 가장 적은 새벽에 하루 한 번. 유예기간이 시간 단위라 분 단위 정확도는 필요 없다. */
    @Scheduled(cron = "0 30 4 * * *")
    public void cleanupOrphanImages() {
        imageCleanupUseCase.cleanupOrphans();
    }
}
