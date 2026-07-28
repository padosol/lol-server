package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import com.example.lolserver.duo.application.port.in.DuoPostExpirationUseCase;
import com.example.lolserver.duo.application.port.out.DuoNotificationPort;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
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
    private final DuoNotificationPort duoNotificationPort;

    @Override
    @Transactional
    public void expireOverduePosts() {
        List<Long> expiredPostIds = duoPostPersistencePort.expireAllOverdue(LocalDateTime.now());

        for (Long postId : expiredPostIds) {
            List<DuoRequest> openRequests = duoRequestPersistencePort
                    .findByDuoPostId(postId).stream()
                    .filter(request -> request.getStatus() == DuoRequestStatus.PENDING
                            || request.getStatus() == DuoRequestStatus.ACCEPTED)
                    .toList();

            duoRequestPersistencePort.closeAllOpen(postId);

            openRequests.forEach(closed -> duoNotificationPort.notify(
                    new DuoNotificationEvent(DuoNotificationType.REQUEST_CLOSED,
                            closed.getRequesterId(), postId, closed.getId())));
        }

        if (!expiredPostIds.isEmpty()) {
            log.info("Expired {} duo posts: {}", expiredPostIds.size(), expiredPostIds);
        }
    }
}
