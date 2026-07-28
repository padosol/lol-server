package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import com.example.lolserver.duo.application.port.out.DuoNotificationPort;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DuoPostExpirationServiceTest {

    @InjectMocks
    private DuoPostExpirationService duoPostExpirationService;

    @Mock
    private DuoPostPersistencePort duoPostPersistencePort;

    @Mock
    private DuoRequestPersistencePort duoRequestPersistencePort;

    @Mock
    private DuoNotificationPort duoNotificationPort;

    @Nested
    @DisplayName("expireOverduePosts")
    class ExpireOverduePosts {

        @DisplayName("만료 대상 게시글이 있으면 각 게시글의 열린 요청을 닫는다")
        @Test
        void overduePosts_closesOpenRequestsForEach() {
            // given
            List<Long> expiredPostIds = List.of(1L, 2L);
            given(duoPostPersistencePort.expireAllOverdue(any(LocalDateTime.class)))
                    .willReturn(expiredPostIds);

            // when
            duoPostExpirationService.expireOverduePosts();

            // then
            then(duoRequestPersistencePort).should().closeAllOpen(1L);
            then(duoRequestPersistencePort).should().closeAllOpen(2L);
        }

        @DisplayName("만료로 닫힌 열린 요청자에게 REQUEST_CLOSED 알림을 발행한다")
        @Test
        void overduePosts_notifiesClosedRequesters() {
            // given
            Long postId = 1L;
            DuoRequest pendingRequest = createRequest(300L, postId, 5L, DuoRequestStatus.PENDING);
            DuoRequest confirmedRequest = createRequest(301L, postId, 6L, DuoRequestStatus.CONFIRMED);
            given(duoPostPersistencePort.expireAllOverdue(any(LocalDateTime.class)))
                    .willReturn(List.of(postId));
            given(duoRequestPersistencePort.findByDuoPostId(postId))
                    .willReturn(List.of(pendingRequest, confirmedRequest));

            // when
            duoPostExpirationService.expireOverduePosts();

            // then
            then(duoNotificationPort).should().notify(new DuoNotificationEvent(
                    DuoNotificationType.REQUEST_CLOSED, 5L, postId, 300L));
            then(duoNotificationPort).should(never()).notify(new DuoNotificationEvent(
                    DuoNotificationType.REQUEST_CLOSED, 6L, postId, 301L));
        }

        @DisplayName("만료 대상이 없으면 요청을 닫지 않는다")
        @Test
        void noOverduePosts_doesNotCloseRequests() {
            // given
            given(duoPostPersistencePort.expireAllOverdue(any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // when
            duoPostExpirationService.expireOverduePosts();

            // then
            then(duoRequestPersistencePort).should(never()).closeAllOpen(anyLong());
        }
    }

    private DuoRequest createRequest(Long id, Long duoPostId, Long requesterId,
            DuoRequestStatus status) {
        return DuoRequest.builder()
                .id(id)
                .duoPostId(duoPostId)
                .requesterId(requesterId)
                .requesterPuuid("requester-puuid")
                .primaryLane(Lane.ADC)
                .desiredLane(Lane.SUPPORT)
                .hasMicrophone(false)
                .tier("SILVER")
                .rank("II")
                .leaguePoints(30)
                .memo("같이 하실 분")
                .status(status)
                .mostChampions(Collections.emptyList())
                .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
