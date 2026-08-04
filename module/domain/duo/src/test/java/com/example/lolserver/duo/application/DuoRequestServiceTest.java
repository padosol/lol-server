package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.duo.application.model.resultmodel.DuoMatchResultModel;
import com.example.lolserver.duo.application.model.readmodel.DuoRequestReadModel;
import com.example.lolserver.duo.application.model.resultmodel.DuoRequestResultModel;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import com.example.lolserver.duo.application.port.out.DuoLockPort;
import com.example.lolserver.duo.application.port.out.DuoNotificationPort;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.duo.domain.vo.MostChampion;
import com.example.lolserver.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.duo.domain.vo.TierInfo;
import com.example.lolserver.summoner.application.model.readmodel.SummonerReadModel;
import com.example.lolserver.summoner.application.port.in.SummonerQueryUseCase;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DuoRequestServiceTest {

    @InjectMocks
    private DuoRequestService duoRequestService;

    @Mock
    private DuoRequestPersistencePort duoRequestPersistencePort;

    @Mock
    private DuoPostPersistencePort duoPostPersistencePort;

    @Mock
    private SummonerQueryUseCase summonerQueryUseCase;

    @Mock
    private RiotAccountResolver riotAccountResolver;

    @Mock
    private DuoLockPort duoLockPort;

    @Mock
    private DuoNotificationPort duoNotificationPort;

    private void givenLockPassThrough() {
        given(duoLockPort.executeWithLock(anyString(), any()))
                .willAnswer(invocation -> {
                    Supplier<?> action = invocation.getArgument(1);
                    return action.get();
                });
    }

    @Nested
    @DisplayName("createDuoRequest")
    class CreateDuoRequest {

        @DisplayName("비활성 게시글에 요청 시 DUO_POST_NOT_ACTIVE 에러")
        @Test
        void inactivePost_throwsException() {
            // given
            Long memberId = 2L;
            Long duoPostId = 100L;
            String puuid = "test-puuid";
            DuoPost duoPost = DuoPost.builder()
                    .id(duoPostId)
                    .memberId(1L)
                    .puuid("owner-puuid")
                    .primaryLane(Lane.MID)
                    .desiredLane(Lane.JUNGLE)
                    .hasMicrophone(true)
                    .tier("GOLD")
                    .rank("I")
                    .leaguePoints(50)
                    .memo("듀오 구합니다")
                    .status(DuoPostStatus.EXPIRED)
                    .mostChampions(Collections.emptyList())
                    .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .build();
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("같이 하실 분")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoRequestService.createDuoRequest(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE);
        }

        @DisplayName("자기 게시글에 요청 시 DUO_POST_SELF_REQUEST 에러")
        @Test
        void selfRequest_throwsException() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            String puuid = "test-puuid";
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("같이 하실 분")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoRequestService.createDuoRequest(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_SELF_REQUEST);
        }

        @DisplayName("중복 요청 시 DUO_REQUEST_ALREADY_EXISTS 에러")
        @Test
        void duplicateRequest_throwsException() {
            // given
            Long memberId = 2L;
            Long duoPostId = 100L;
            String puuid = "test-puuid";
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("같이 하실 분")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.existsByDuoPostIdAndRequesterIdAndStatusIn(
                    eq(duoPostId), eq(memberId), anyList()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> duoRequestService.createDuoRequest(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_REQUEST_ALREADY_EXISTS);
        }

        @DisplayName("정상 요청 생성")
        @Test
        void success() {
            // given
            Long memberId = 2L;
            Long duoPostId = 100L;
            String puuid = "test-puuid";
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            TierInfo tierInfo = new TierInfo("GOLD", "I", 50);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(true)
                    .memo("같이 하실 분")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.existsByDuoPostIdAndRequesterIdAndStatusIn(
                    eq(duoPostId), eq(memberId), anyList()))
                    .willReturn(false);
            List<MostChampion> mostChampions = List.of(
                    new MostChampion(1, "Jinx", 30, 18, 12));
            RecentGameSummary recentGameSummary = new RecentGameSummary(10, 10, List.of(
                    new RecentGameSummary.PlayedChampion(1, "Jinx")));
            RiotAccountStats stats = new RiotAccountStats(tierInfo, mostChampions, recentGameSummary);

            given(riotAccountResolver.lookupAllStats(puuid)).willReturn(stats);
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willAnswer(invocation -> {
                        DuoRequest request = invocation.getArgument(0);
                        return DuoRequest.builder()
                                .id(200L)
                                .duoPostId(request.getDuoPostId())
                                .requesterId(request.getRequesterId())
                                .requesterPuuid(request.getRequesterPuuid())
                                .primaryLane(request.getPrimaryLane())
                                .desiredLane(request.getDesiredLane())
                                .hasMicrophone(request.isHasMicrophone())
                                .tier(request.getTier())
                                .rank(request.getRank())
                                .leaguePoints(request.getLeaguePoints())
                                .memo(request.getMemo())
                                .status(request.getStatus())
                                .mostChampions(request.getMostChampions())
                                .recentGameSummary(request.getRecentGameSummary())
                                .createdAt(request.getCreatedAt())
                                .updatedAt(request.getUpdatedAt())
                                .build();
                    });

            // when
            DuoRequestResultModel result = duoRequestService.createDuoRequest(memberId, duoPostId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(200L);
            assertThat(result.getDuoPostId()).isEqualTo(duoPostId);
            assertThat(result.getPrimaryLane()).isEqualTo("ADC");
            assertThat(result.getDesiredLane()).isEqualTo("SUPPORT");
            assertThat(result.isHasMicrophone()).isTrue();
            assertThat(result.getTier()).isEqualTo("GOLD");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getMostChampions()).hasSize(1);
            assertThat(result.getRecentGameSummary().wins()).isEqualTo(10);
            then(duoRequestPersistencePort).should().save(any(DuoRequest.class));
        }

        @DisplayName("솔로랭크 티어가 없으면 DUO_UNRANKED_NOT_ALLOWED 에러 - 저장되지 않는다")
        @Test
        void unranked_throwsException() {
            // given
            Long memberId = 2L;
            Long duoPostId = 100L;
            String puuid = "test-puuid";
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(true)
                    .memo("같이 하실 분")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.existsByDuoPostIdAndRequesterIdAndStatusIn(
                    eq(duoPostId), eq(memberId), anyList()))
                    .willReturn(false);
            given(riotAccountResolver.lookupAllStats(puuid)).willReturn(new RiotAccountStats(
                    TierInfo.UNRANKED, Collections.emptyList(),
                    new RecentGameSummary(0, 0, Collections.emptyList())));

            // when & then
            assertThatThrownBy(() ->
                    duoRequestService.createDuoRequest(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_UNRANKED_NOT_ALLOWED);
            then(duoRequestPersistencePort).should(never()).save(any(DuoRequest.class));
        }
    }

    @Nested
    @DisplayName("acceptDuoRequest")
    class AcceptDuoRequest {

        @DisplayName("소유자가 아닌 경우 FORBIDDEN 에러")
        @Test
        void notOwner_throwsForbidden() {
            // given
            Long memberId = 3L;
            Long requestId = 200L;
            Long duoPostId = 100L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, duoPostId, 2L);
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoRequestService.acceptDuoRequest(memberId, requestId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN);

            then(duoRequestPersistencePort).should(never()).save(any(DuoRequest.class));
        }

        @DisplayName("정상 수락 - status ACCEPTED")
        @Test
        void success() {
            // given
            Long memberId = 1L;
            Long requestId = 200L;
            Long duoPostId = 100L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, duoPostId, 2L);
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willReturn(duoRequest);

            // when
            DuoMatchResultModel result = duoRequestService.acceptDuoRequest(memberId, requestId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getDuoPostId()).isEqualTo(duoPostId);
            assertThat(result.getRequestId()).isEqualTo(requestId);
            assertThat(result.getStatus()).isEqualTo("ACCEPTED");
            assertThat(result.getPartnerGameName()).isNull();
            assertThat(result.getPartnerTagLine()).isNull();
            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.ACCEPTED);
            then(duoRequestPersistencePort).should().save(duoRequest);
            then(duoNotificationPort).should().notify(new DuoNotificationEvent(
                    DuoNotificationType.REQUEST_ACCEPTED, 2L, duoPostId, requestId));
        }
    }

    @Nested
    @DisplayName("confirmDuoRequest")
    class ConfirmDuoRequest {

        @DisplayName("요청자가 아닌 경우 FORBIDDEN 에러")
        @Test
        void notRequester_throwsForbidden() {
            // given
            Long memberId = 3L;
            Long requestId = 200L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, 100L, 2L);
            duoRequest.accept();

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));

            // when & then
            assertThatThrownBy(() -> duoRequestService.confirmDuoRequest(memberId, requestId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN);
        }

        @DisplayName("정상 확인 - 매칭 완료, gameName 반환, 나머지 요청 자동 종료")
        @Test
        void success() {
            // given
            Long requesterId = 2L;
            Long requestId = 200L;
            Long duoPostId = 100L;
            String ownerPuuid = "owner-puuid";
            DuoRequest duoRequest = createTestDuoRequest(requestId, duoPostId, requesterId);
            duoRequest.accept();
            DuoPost duoPost = DuoPost.builder()
                    .id(duoPostId)
                    .memberId(1L)
                    .puuid(ownerPuuid)
                    .primaryLane(Lane.MID)
                    .desiredLane(Lane.JUNGLE)
                    .hasMicrophone(true)
                    .tier("GOLD")
                    .rank("I")
                    .leaguePoints(50)
                    .memo("듀오 구합니다")
                    .status(DuoPostStatus.ACTIVE)
                    .mostChampions(Collections.emptyList())
                    .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            SummonerReadModel partnerSummoner = SummonerReadModel.builder()
                    .puuid(ownerPuuid)
                    .gameName("Hide on bush")
                    .tagLine("KR1")
                    .build();

            DuoRequest losingRequest = createTestDuoRequest(201L, duoPostId, 3L);

            givenLockPassThrough();
            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willReturn(duoRequest);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoPostPersistencePort.markMatchedIfActive(duoPostId))
                    .willReturn(true);
            given(duoRequestPersistencePort.findByDuoPostId(duoPostId))
                    .willReturn(List.of(duoRequest, losingRequest));
            given(summonerQueryUseCase.findSummonerByPuuid(ownerPuuid))
                    .willReturn(Optional.of(partnerSummoner));

            // when
            DuoMatchResultModel result = duoRequestService.confirmDuoRequest(requesterId, requestId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getDuoPostId()).isEqualTo(duoPostId);
            assertThat(result.getRequestId()).isEqualTo(requestId);
            assertThat(result.getPartnerGameName()).isEqualTo("Hide on bush");
            assertThat(result.getPartnerTagLine()).isEqualTo("KR1");
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.CONFIRMED);
            assertThat(duoPost.getStatus()).isEqualTo(DuoPostStatus.MATCHED);
            then(duoRequestPersistencePort).should()
                    .closeAllOpenExcept(duoPostId, requestId);
            then(duoNotificationPort).should().notify(new DuoNotificationEvent(
                    DuoNotificationType.MATCH_CONFIRMED, 1L, duoPostId, requestId));
            then(duoNotificationPort).should().notify(new DuoNotificationEvent(
                    DuoNotificationType.MATCH_CONFIRMED, requesterId, duoPostId, requestId));
            then(duoNotificationPort).should().notify(new DuoNotificationEvent(
                    DuoNotificationType.REQUEST_CLOSED, 3L, duoPostId, 201L));
        }

        @DisplayName("duo:post:{duoPostId} 키의 락 안에서 매칭 확정이 실행된다")
        @Test
        void executesWithinPostLock() {
            // given
            Long requesterId = 2L;
            Long requestId = 200L;
            Long duoPostId = 100L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, duoPostId, requesterId);
            duoRequest.accept();
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);

            givenLockPassThrough();
            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willReturn(duoRequest);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoPostPersistencePort.markMatchedIfActive(duoPostId))
                    .willReturn(true);
            given(summonerQueryUseCase.findSummonerByPuuid("owner-puuid"))
                    .willReturn(Optional.empty());

            // when
            duoRequestService.confirmDuoRequest(requesterId, requestId);

            // then
            then(duoLockPort).should()
                    .executeWithLock(eq("duo:post:" + duoPostId), any());
        }

        @DisplayName("락 획득 실패 시 LOCK_ACQUISITION_FAILED 에러 - 요청 상태는 변경되지 않는다")
        @Test
        void lockAcquisitionFailed_throwsException() {
            // given
            Long requesterId = 2L;
            Long requestId = 200L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, 100L, requesterId);
            duoRequest.accept();

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoLockPort.executeWithLock(anyString(), any()))
                    .willThrow(new CoreException(ErrorType.LOCK_ACQUISITION_FAILED));

            // when & then
            assertThatThrownBy(() -> duoRequestService.confirmDuoRequest(requesterId, requestId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.LOCK_ACQUISITION_FAILED);

            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.ACCEPTED);
            then(duoRequestPersistencePort).should(never()).save(any(DuoRequest.class));
        }

        @DisplayName("락 획득 후 게시글이 이미 MATCHED면 DUO_POST_NOT_ACTIVE - 요청은 CONFIRMED 되지 않는다")
        @Test
        void alreadyMatchedPost_throwsNotActive() {
            // given
            Long requesterId = 2L;
            Long requestId = 200L;
            Long duoPostId = 100L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, duoPostId, requesterId);
            duoRequest.accept();
            DuoPost matchedPost = createTestDuoPost(duoPostId, 1L);
            matchedPost.markMatched();

            givenLockPassThrough();
            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(matchedPost));

            // when & then
            assertThatThrownBy(() -> duoRequestService.confirmDuoRequest(requesterId, requestId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE);

            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.ACCEPTED);
            then(duoRequestPersistencePort).should(never()).save(any(DuoRequest.class));
        }
    }

    @Nested
    @DisplayName("confirmDuoRequest 선착순 동시성")
    class ConfirmDuoRequestConcurrency {

        @DisplayName("두 요청자가 동시에 승락해도 1명만 CONFIRMED 되고 나머지는 DUO_POST_NOT_ACTIVE")
        @Test
        void onlyOneConfirmed() throws Exception {
            // given
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            DuoRequest request1 = createTestDuoRequest(200L, duoPostId, 2L);
            DuoRequest request2 = createTestDuoRequest(201L, duoPostId, 3L);
            request1.accept();
            request2.accept();

            ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
            given(duoLockPort.executeWithLock(anyString(), any()))
                    .willAnswer(invocation -> {
                        ReentrantLock lock = locks.computeIfAbsent(
                                invocation.getArgument(0), key -> new ReentrantLock());
                        lock.lock();
                        try {
                            Supplier<?> action = invocation.getArgument(1);
                            return action.get();
                        } finally {
                            lock.unlock();
                        }
                    });
            given(duoRequestPersistencePort.findById(200L))
                    .willReturn(Optional.of(request1));
            given(duoRequestPersistencePort.findById(201L))
                    .willReturn(Optional.of(request2));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(duoPostPersistencePort.markMatchedIfActive(duoPostId))
                    .willReturn(true);
            given(summonerQueryUseCase.findSummonerByPuuid("owner-puuid"))
                    .willReturn(Optional.empty());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            Future<Object> first = executor.submit(confirmTask(startLatch, 2L, 200L));
            Future<Object> second = executor.submit(confirmTask(startLatch, 3L, 201L));

            // when
            startLatch.countDown();
            Object result1 = first.get(5, TimeUnit.SECONDS);
            Object result2 = second.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();

            // then
            List<Object> results = List.of(result1, result2);
            assertThat(results).filteredOn(DuoMatchResultModel.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(CoreException.class::isInstance)
                    .hasSize(1)
                    .allSatisfy(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE));
            long confirmedCount = List.of(request1, request2).stream()
                    .filter(request -> request.getStatus() == DuoRequestStatus.CONFIRMED)
                    .count();
            assertThat(confirmedCount).isEqualTo(1);
            assertThat(duoPost.getStatus()).isEqualTo(DuoPostStatus.MATCHED);
        }

        private Callable<Object> confirmTask(CountDownLatch startLatch, Long memberId, Long requestId) {
            return () -> {
                startLatch.await();
                try {
                    return duoRequestService.confirmDuoRequest(memberId, requestId);
                } catch (CoreException e) {
                    return e;
                }
            };
        }
    }

    @Nested
    @DisplayName("getMatchResult")
    class GetMatchResult {

        @DisplayName("소유자 조회 시 상대(확정 요청자)의 게임이름을 반환한다")
        @Test
        void owner_returnsRequesterIdentity() {
            // given
            Long ownerId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, ownerId);
            duoPost.markMatched();
            DuoRequest confirmedRequest = createTestDuoRequest(200L, duoPostId, 2L);
            confirmedRequest.accept();
            confirmedRequest.confirm();
            SummonerReadModel requesterSummoner = SummonerReadModel.builder()
                    .puuid("requester-puuid")
                    .gameName("DuoBuddy")
                    .tagLine("KR2")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.findConfirmedByDuoPostId(duoPostId))
                    .willReturn(Optional.of(confirmedRequest));
            given(summonerQueryUseCase.findSummonerByPuuid("requester-puuid"))
                    .willReturn(Optional.of(requesterSummoner));

            // when
            DuoMatchResultModel result = duoRequestService.getMatchResult(ownerId, duoPostId);

            // then
            assertThat(result.getDuoPostId()).isEqualTo(duoPostId);
            assertThat(result.getRequestId()).isEqualTo(200L);
            assertThat(result.getPartnerGameName()).isEqualTo("DuoBuddy");
            assertThat(result.getPartnerTagLine()).isEqualTo("KR2");
            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        }

        @DisplayName("확정 요청자 조회 시 상대(소유자)의 게임이름을 반환한다")
        @Test
        void confirmedRequester_returnsOwnerIdentity() {
            // given
            Long requesterId = 2L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            duoPost.markMatched();
            DuoRequest confirmedRequest = createTestDuoRequest(200L, duoPostId, requesterId);
            confirmedRequest.accept();
            confirmedRequest.confirm();
            SummonerReadModel ownerSummoner = SummonerReadModel.builder()
                    .puuid("owner-puuid")
                    .gameName("Hide on bush")
                    .tagLine("KR1")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.findConfirmedByDuoPostId(duoPostId))
                    .willReturn(Optional.of(confirmedRequest));
            given(summonerQueryUseCase.findSummonerByPuuid("owner-puuid"))
                    .willReturn(Optional.of(ownerSummoner));

            // when
            DuoMatchResultModel result = duoRequestService.getMatchResult(requesterId, duoPostId);

            // then
            assertThat(result.getPartnerGameName()).isEqualTo("Hide on bush");
            assertThat(result.getPartnerTagLine()).isEqualTo("KR1");
        }

        @DisplayName("MATCHED 상태가 아닌 게시글 조회 시 DUO_POST_NOT_MATCHED 에러")
        @Test
        void notMatchedPost_throwsException() {
            // given
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoRequestService.getMatchResult(1L, duoPostId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_NOT_MATCHED);
        }

        @DisplayName("매칭 당사자가 아니면 FORBIDDEN 에러")
        @Test
        void thirdParty_throwsForbidden() {
            // given
            Long thirdPartyId = 3L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            duoPost.markMatched();
            DuoRequest confirmedRequest = createTestDuoRequest(200L, duoPostId, 2L);
            confirmedRequest.accept();
            confirmedRequest.confirm();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.findConfirmedByDuoPostId(duoPostId))
                    .willReturn(Optional.of(confirmedRequest));

            // when & then
            assertThatThrownBy(() -> duoRequestService.getMatchResult(thirdPartyId, duoPostId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("rejectDuoRequest")
    class RejectDuoRequest {

        @DisplayName("정상 거절")
        @Test
        void success() {
            // given
            Long memberId = 1L;
            Long requestId = 200L;
            Long duoPostId = 100L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, duoPostId, 2L);
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willReturn(duoRequest);

            // when
            duoRequestService.rejectDuoRequest(memberId, requestId);

            // then
            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.REJECTED);
            then(duoRequestPersistencePort).should().save(duoRequest);
        }
    }

    @Nested
    @DisplayName("cancelDuoRequest")
    class CancelDuoRequest {

        @DisplayName("정상 취소")
        @Test
        void success() {
            // given
            Long requesterId = 2L;
            Long requestId = 200L;
            DuoRequest duoRequest = createTestDuoRequest(requestId, 100L, requesterId);

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willReturn(duoRequest);

            // when
            duoRequestService.cancelDuoRequest(requesterId, requestId);

            // then
            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.CANCELLED);
            then(duoRequestPersistencePort).should().save(duoRequest);
        }
    }

    @Nested
    @DisplayName("getDuoRequestsForPost")
    class GetDuoRequestsForPost {

        @DisplayName("소유자가 아닌 경우 FORBIDDEN 에러")
        @Test
        void notOwner_throwsForbidden() {
            // given
            Long memberId = 2L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoRequestService.getDuoRequestsForPost(memberId, duoPostId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN);
        }

        @DisplayName("소유자 조회 시 요청 목록 반환")
        @Test
        void owner_returnsRequests() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);
            DuoRequest request = createTestDuoRequest(200L, duoPostId, 2L);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.findByDuoPostId(duoPostId))
                    .willReturn(List.of(request));

            // when
            List<DuoRequestReadModel> result = duoRequestService.getDuoRequestsForPost(memberId, duoPostId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(200L);
            assertThat(result.get(0).getDuoPostId()).isEqualTo(duoPostId);
        }
    }

    // --- 테스트 헬퍼 메서드 ---

    private DuoPost createTestDuoPost(Long id, Long memberId) {
        return DuoPost.builder()
                .id(id)
                .memberId(memberId)
                .puuid("owner-puuid")
                .primaryLane(Lane.MID)
                .desiredLane(Lane.JUNGLE)
                .hasMicrophone(true)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(50)
                .memo("듀오 구합니다")
                .status(DuoPostStatus.ACTIVE)
                .mostChampions(Collections.emptyList())
                .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private DuoRequest createTestDuoRequest(Long id, Long duoPostId, Long requesterId) {
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
                .status(DuoRequestStatus.PENDING)
                .mostChampions(Collections.emptyList())
                .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
