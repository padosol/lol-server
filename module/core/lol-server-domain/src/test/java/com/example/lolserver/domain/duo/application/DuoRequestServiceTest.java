package com.example.lolserver.domain.duo.application;

import com.example.lolserver.domain.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.domain.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.domain.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.domain.duo.domain.vo.TierInfo;
import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private SummonerPersistencePort summonerPersistencePort;

    @Mock
    private RiotAccountResolver riotAccountResolver;

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

            given(riotAccountResolver.lookupTierInfo(puuid)).willReturn(tierInfo);
            given(riotAccountResolver.lookupMostChampions(puuid)).willReturn(mostChampions);
            given(riotAccountResolver.lookupRecentGameSummary(puuid)).willReturn(recentGameSummary);
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
            DuoRequestReadModel result = duoRequestService.createDuoRequest(memberId, duoPostId, command);

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
            DuoMatchResultReadModel result = duoRequestService.acceptDuoRequest(memberId, requestId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getDuoPostId()).isEqualTo(duoPostId);
            assertThat(result.getRequestId()).isEqualTo(requestId);
            assertThat(result.getStatus()).isEqualTo("ACCEPTED");
            assertThat(result.getPartnerGameName()).isNull();
            assertThat(result.getPartnerTagLine()).isNull();
            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.ACCEPTED);
            then(duoRequestPersistencePort).should().save(duoRequest);
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

        @DisplayName("정상 확인 - 매칭 완료, gameName 반환, 나머지 요청 자동 거절")
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
            Summoner partnerSummoner = Summoner.builder()
                    .puuid(ownerPuuid)
                    .gameName("Hide on bush")
                    .tagLine("KR1")
                    .build();

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willReturn(duoRequest);
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willReturn(duoPost);
            given(summonerPersistencePort.findById(ownerPuuid))
                    .willReturn(Optional.of(partnerSummoner));

            // when
            DuoMatchResultReadModel result = duoRequestService.confirmDuoRequest(requesterId, requestId);

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
                    .rejectAllPendingAndAccepted(duoPostId, requestId);
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
