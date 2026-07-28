package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.command.CreateDuoPostCommand;
import com.example.lolserver.duo.application.command.UpdateDuoPostCommand;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import com.example.lolserver.duo.application.model.readmodel.DuoPostDetailReadModel;
import com.example.lolserver.duo.application.model.resultmodel.DuoPostResultModel;
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
import java.util.ArrayList;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DuoServiceTest {

    @InjectMocks
    private DuoService duoService;

    @Mock
    private DuoPostPersistencePort duoPostPersistencePort;

    @Mock
    private DuoRequestPersistencePort duoRequestPersistencePort;

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
    @DisplayName("createDuoPost")
    class CreateDuoPost {

        @DisplayName("Riot 미연동 시 RIOT_ACCOUNT_NOT_LINKED 에러")
        @Test
        void riotNotLinked_throwsException() {
            // given
            Long memberId = 1L;
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .desiredLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId))
                    .willThrow(new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));

            // when & then
            assertThatThrownBy(() -> duoService.createDuoPost(memberId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.RIOT_ACCOUNT_NOT_LINKED);
        }

        @DisplayName("이미 활성 게시글이 있으면 DUO_POST_ACTIVE_EXISTS 에러")
        @Test
        void activePostExists_throwsException() {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .desiredLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();

            givenLockPassThrough();
            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.existsActiveByMemberId(memberId))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> duoService.createDuoPost(memberId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_ACTIVE_EXISTS);

            then(duoPostPersistencePort).should(never()).save(any(DuoPost.class));
        }

        @DisplayName("정상 등록 - 티어 정보 있을 때 tierAvailable=true")
        @Test
        void success_withTier() {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            TierInfo tierInfo = new TierInfo("GOLD", "I", 50);
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .desiredLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();

            List<MostChampion> mostChampions = List.of(
                    new MostChampion(1, "Ahri", 50, 30, 20));
            RecentGameSummary recentGameSummary = new RecentGameSummary(12, 8, List.of(
                    new RecentGameSummary.PlayedChampion(1, "Ahri")));
            RiotAccountStats stats = new RiotAccountStats(tierInfo, mostChampions, recentGameSummary);

            givenLockPassThrough();
            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.existsActiveByMemberId(memberId))
                    .willReturn(false);
            given(riotAccountResolver.lookupAllStats(puuid)).willReturn(stats);
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willAnswer(invocation -> {
                        DuoPost post = invocation.getArgument(0);
                        return DuoPost.builder()
                                .id(100L)
                                .memberId(post.getMemberId())
                                .puuid(post.getPuuid())
                                .primaryLane(post.getPrimaryLane())
                                .desiredLane(post.getDesiredLane())
                                .hasMicrophone(post.isHasMicrophone())
                                .tier(post.getTier())
                                .rank(post.getRank())
                                .leaguePoints(post.getLeaguePoints())
                                .memo(post.getMemo())
                                .status(post.getStatus())
                                .mostChampions(post.getMostChampions())
                                .recentGameSummary(post.getRecentGameSummary())
                                .expiresAt(post.getExpiresAt())
                                .createdAt(post.getCreatedAt())
                                .updatedAt(post.getUpdatedAt())
                                .build();
                    });

            // when
            DuoPostResultModel result = duoService.createDuoPost(memberId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getPrimaryLane()).isEqualTo("MID");
            assertThat(result.getDesiredLane()).isEqualTo("JUNGLE");
            assertThat(result.isHasMicrophone()).isTrue();
            assertThat(result.getTier()).isEqualTo("GOLD");
            assertThat(result.getRank()).isEqualTo("I");
            assertThat(result.getLeaguePoints()).isEqualTo(50);
            assertThat(result.isTierAvailable()).isTrue();
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getMostChampions()).hasSize(1);
            assertThat(result.getRecentGameSummary().wins()).isEqualTo(12);
            then(duoPostPersistencePort).should().save(any(DuoPost.class));
            then(duoLockPort).should()
                    .executeWithLock(eq("duo:member:post:" + memberId), any());
        }

        @DisplayName("티어 정보 없을 때 tierAvailable=false")
        @Test
        void success_withoutTier() {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("티어 없음")
                    .build();

            RiotAccountStats stats = new RiotAccountStats(
                    TierInfo.UNRANKED, Collections.emptyList(),
                    new RecentGameSummary(0, 0, Collections.emptyList()));

            givenLockPassThrough();
            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoPostPersistencePort.existsActiveByMemberId(memberId))
                    .willReturn(false);
            given(riotAccountResolver.lookupAllStats(puuid)).willReturn(stats);
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willAnswer(invocation -> {
                        DuoPost post = invocation.getArgument(0);
                        return DuoPost.builder()
                                .id(101L)
                                .memberId(post.getMemberId())
                                .puuid(post.getPuuid())
                                .primaryLane(post.getPrimaryLane())
                                .desiredLane(post.getDesiredLane())
                                .hasMicrophone(post.isHasMicrophone())
                                .tier(post.getTier())
                                .rank(post.getRank())
                                .leaguePoints(post.getLeaguePoints())
                                .memo(post.getMemo())
                                .status(post.getStatus())
                                .mostChampions(post.getMostChampions())
                                .recentGameSummary(post.getRecentGameSummary())
                                .expiresAt(post.getExpiresAt())
                                .createdAt(post.getCreatedAt())
                                .updatedAt(post.getUpdatedAt())
                                .build();
                    });

            // when
            DuoPostResultModel result = duoService.createDuoPost(memberId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTier()).isNull();
            assertThat(result.getRank()).isNull();
            assertThat(result.isTierAvailable()).isFalse();
        }

        @DisplayName("락 획득 실패 시 LOCK_ACQUISITION_FAILED 에러 - 게시글은 저장되지 않는다")
        @Test
        void lockAcquisitionFailed_throwsException() {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .desiredLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();

            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(duoLockPort.executeWithLock(anyString(), any()))
                    .willThrow(new CoreException(ErrorType.LOCK_ACQUISITION_FAILED));

            // when & then
            assertThatThrownBy(() -> duoService.createDuoPost(memberId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.LOCK_ACQUISITION_FAILED);

            then(duoPostPersistencePort).should(never()).save(any(DuoPost.class));
        }
    }

    @Nested
    @DisplayName("createDuoPost 활성글 1개 동시성")
    class CreateDuoPostConcurrency {

        @DisplayName("동시에 두 번 생성 요청해도 1개만 저장되고 나머지는 DUO_POST_ACTIVE_EXISTS")
        @Test
        void onlyOnePostCreated() throws Exception {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .desiredLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();
            RiotAccountStats stats = new RiotAccountStats(
                    TierInfo.UNRANKED, Collections.emptyList(),
                    new RecentGameSummary(0, 0, Collections.emptyList()));
            List<DuoPost> savedPosts = Collections.synchronizedList(new ArrayList<>());

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
            given(riotAccountResolver.extractRiotPuuid(memberId)).willReturn(puuid);
            given(riotAccountResolver.lookupAllStats(puuid)).willReturn(stats);
            given(duoPostPersistencePort.existsActiveByMemberId(memberId))
                    .willAnswer(invocation -> !savedPosts.isEmpty());
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willAnswer(invocation -> {
                        DuoPost post = invocation.getArgument(0);
                        savedPosts.add(post);
                        return post;
                    });

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            Future<Object> first = executor.submit(createTask(startLatch, memberId, command));
            Future<Object> second = executor.submit(createTask(startLatch, memberId, command));

            // when
            startLatch.countDown();
            Object result1 = first.get(5, TimeUnit.SECONDS);
            Object result2 = second.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();

            // then
            List<Object> results = List.of(result1, result2);
            assertThat(results).filteredOn(DuoPostResultModel.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(CoreException.class::isInstance)
                    .hasSize(1)
                    .allSatisfy(e -> assertThat(((CoreException) e).getErrorType())
                            .isEqualTo(ErrorType.DUO_POST_ACTIVE_EXISTS));
            assertThat(savedPosts).hasSize(1);
        }

        private Callable<Object> createTask(CountDownLatch startLatch, Long memberId,
                CreateDuoPostCommand command) {
            return () -> {
                startLatch.await();
                try {
                    return duoService.createDuoPost(memberId, command);
                } catch (CoreException e) {
                    return e;
                }
            };
        }
    }

    @Nested
    @DisplayName("deleteDuoPost")
    class DeleteDuoPost {

        @DisplayName("소유자가 아닌 경우 FORBIDDEN 에러")
        @Test
        void notOwner_throwsForbidden() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, otherMemberId);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoService.deleteDuoPost(memberId, duoPostId))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN);

            then(duoPostPersistencePort).should(never()).save(any(DuoPost.class));
        }

        @DisplayName("정상 삭제 - markDeleted 후 save")
        @Test
        void success() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);
            DuoRequest openRequest = createTestDuoRequest(200L, duoPostId, 2L);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willReturn(duoPost);
            given(duoRequestPersistencePort.findByDuoPostId(duoPostId))
                    .willReturn(List.of(openRequest));

            // when
            duoService.deleteDuoPost(memberId, duoPostId);

            // then
            assertThat(duoPost.getStatus()).isEqualTo(DuoPostStatus.DELETED);
            then(duoPostPersistencePort).should().save(duoPost);
            then(duoRequestPersistencePort).should().closeAllOpen(duoPostId);
            then(duoNotificationPort).should().notify(new DuoNotificationEvent(
                    DuoNotificationType.REQUEST_CLOSED, 2L, duoPostId, 200L));
        }
    }

    @Nested
    @DisplayName("updateDuoPost")
    class UpdateDuoPost {

        @DisplayName("존재하지 않는 게시글 수정 시 DUO_POST_NOT_FOUND 에러")
        @Test
        void postNotFound_throwsException() {
            // given
            Long memberId = 1L;
            Long duoPostId = 999L;
            UpdateDuoPostCommand command = UpdateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("수정된 메모")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> duoService.updateDuoPost(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_NOT_FOUND);
        }

        @DisplayName("소유자가 아닌 경우 FORBIDDEN 에러")
        @Test
        void notOwner_throwsForbidden() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, otherMemberId);
            UpdateDuoPostCommand command = UpdateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("수정된 메모")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoService.updateDuoPost(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.FORBIDDEN);

            then(duoPostPersistencePort).should(never()).save(any(DuoPost.class));
        }

        @DisplayName("MATCHED 상태 게시글 수정 시 DUO_POST_NOT_ACTIVE 에러")
        @Test
        void matchedPost_throwsNotActive() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId,
                    DuoPostStatus.MATCHED, LocalDateTime.now().plusHours(1));
            UpdateDuoPostCommand command = UpdateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("수정된 메모")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoService.updateDuoPost(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE);

            then(duoPostPersistencePort).should(never()).save(any(DuoPost.class));
        }

        @DisplayName("만료된 게시글 수정 시 DUO_POST_NOT_ACTIVE 에러")
        @Test
        void expiredPost_throwsNotActive() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId,
                    DuoPostStatus.ACTIVE, LocalDateTime.now().minusHours(1));
            UpdateDuoPostCommand command = UpdateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("수정된 메모")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoService.updateDuoPost(memberId, duoPostId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.DUO_POST_NOT_ACTIVE);

            then(duoPostPersistencePort).should(never()).save(any(DuoPost.class));
        }

        @DisplayName("정상 수정 - 라인, 마이크, 메모 변경 반영")
        @Test
        void success() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);
            UpdateDuoPostCommand command = UpdateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .desiredLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("수정된 메모")
                    .build();

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            DuoPostResultModel result = duoService.updateDuoPost(memberId, duoPostId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getPrimaryLane()).isEqualTo("TOP");
            assertThat(result.getDesiredLane()).isEqualTo("SUPPORT");
            assertThat(result.isHasMicrophone()).isFalse();
            assertThat(result.getMemo()).isEqualTo("수정된 메모");
            assertThat(result.getTier()).isEqualTo("GOLD");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            then(duoPostPersistencePort).should().save(any(DuoPost.class));
        }
    }

    @Nested
    @DisplayName("getDuoPost")
    class GetDuoPost {

        @DisplayName("소유자 조회 시 requests 포함")
        @Test
        void owner_includesRequests() {
            // given
            Long memberId = 1L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);
            DuoRequest request1 = createTestDuoRequest(200L, duoPostId, 2L);
            DuoRequest request2 = createTestDuoRequest(201L, duoPostId, 3L);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.findByDuoPostId(duoPostId))
                    .willReturn(List.of(request1, request2));

            // when
            DuoPostDetailReadModel result = duoService.getDuoPost(duoPostId, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(duoPostId);
            assertThat(result.isOwner()).isTrue();
            assertThat(result.getRequests()).hasSize(2);
            then(duoRequestPersistencePort).should().findByDuoPostId(duoPostId);
        }

        @DisplayName("비소유자 조회 시 requests 빈 리스트")
        @Test
        void notOwner_emptyRequests() {
            // given
            Long memberId = 2L;
            Long duoPostId = 100L;
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when
            DuoPostDetailReadModel result = duoService.getDuoPost(duoPostId, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isOwner()).isFalse();
            assertThat(result.getRequests()).isEmpty();
            then(duoRequestPersistencePort).should(never()).findByDuoPostId(anyLong());
        }
    }

    // --- 테스트 헬퍼 메서드 ---

    private DuoPost createTestDuoPost(Long id, Long memberId,
            DuoPostStatus status, LocalDateTime expiresAt) {
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
                .status(status)
                .mostChampions(Collections.emptyList())
                .recentGameSummary(new RecentGameSummary(0, 0, Collections.emptyList()))
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

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
