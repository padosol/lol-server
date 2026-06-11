package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
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
}
