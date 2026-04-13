package com.example.lolserver.domain.duo.application;

import com.example.lolserver.domain.duo.application.command.CreateDuoPostCommand;
import com.example.lolserver.domain.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.domain.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostDetailReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.domain.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.SocialAccount;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private MemberPersistencePort memberPersistencePort;

    @Mock
    private LeaguePersistencePort leaguePersistencePort;

    @Mock
    private SummonerPersistencePort summonerPersistencePort;

    @Nested
    @DisplayName("createDuoPost")
    class CreateDuoPost {

        @DisplayName("Riot 미연동 시 RIOT_ACCOUNT_NOT_LINKED 에러")
        @Test
        void riotNotLinked_throwsException() {
            // given
            Long memberId = 1L;
            Member member = createTestMember(memberId, false);
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .secondaryLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> duoService.createDuoPost(memberId, command))
                    .isInstanceOf(CoreException.class)
                    .extracting(e -> ((CoreException) e).getErrorType())
                    .isEqualTo(ErrorType.RIOT_ACCOUNT_NOT_LINKED);
        }

        @DisplayName("정상 등록 - 티어 정보 있을 때 tierAvailable=true")
        @Test
        void success_withTier() {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            Member member = createTestMember(memberId, true);
            League league = createTestLeague(puuid);
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("MID")
                    .secondaryLane("JUNGLE")
                    .hasMicrophone(true)
                    .memo("듀오 구합니다")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));
            given(leaguePersistencePort.findAllLeaguesByPuuid(puuid))
                    .willReturn(List.of(league));
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willAnswer(invocation -> {
                        DuoPost post = invocation.getArgument(0);
                        return DuoPost.builder()
                                .id(100L)
                                .memberId(post.getMemberId())
                                .puuid(post.getPuuid())
                                .primaryLane(post.getPrimaryLane())
                                .secondaryLane(post.getSecondaryLane())
                                .hasMicrophone(post.isHasMicrophone())
                                .tier(post.getTier())
                                .rank(post.getRank())
                                .leaguePoints(post.getLeaguePoints())
                                .memo(post.getMemo())
                                .status(post.getStatus())
                                .expiresAt(post.getExpiresAt())
                                .createdAt(post.getCreatedAt())
                                .updatedAt(post.getUpdatedAt())
                                .build();
                    });

            // when
            DuoPostReadModel result = duoService.createDuoPost(memberId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getPrimaryLane()).isEqualTo("MID");
            assertThat(result.getSecondaryLane()).isEqualTo("JUNGLE");
            assertThat(result.isHasMicrophone()).isTrue();
            assertThat(result.getTier()).isEqualTo("GOLD");
            assertThat(result.getRank()).isEqualTo("I");
            assertThat(result.getLeaguePoints()).isEqualTo(50);
            assertThat(result.isTierAvailable()).isTrue();
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            then(duoPostPersistencePort).should().save(any(DuoPost.class));
        }

        @DisplayName("티어 정보 없을 때 tierAvailable=false")
        @Test
        void success_withoutTier() {
            // given
            Long memberId = 1L;
            String puuid = "test-puuid";
            Member member = createTestMember(memberId, true);
            CreateDuoPostCommand command = CreateDuoPostCommand.builder()
                    .primaryLane("TOP")
                    .secondaryLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("티어 없음")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));
            given(leaguePersistencePort.findAllLeaguesByPuuid(puuid))
                    .willReturn(Collections.emptyList());
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willAnswer(invocation -> {
                        DuoPost post = invocation.getArgument(0);
                        return DuoPost.builder()
                                .id(101L)
                                .memberId(post.getMemberId())
                                .puuid(post.getPuuid())
                                .primaryLane(post.getPrimaryLane())
                                .secondaryLane(post.getSecondaryLane())
                                .hasMicrophone(post.isHasMicrophone())
                                .tier(post.getTier())
                                .rank(post.getRank())
                                .leaguePoints(post.getLeaguePoints())
                                .memo(post.getMemo())
                                .status(post.getStatus())
                                .expiresAt(post.getExpiresAt())
                                .createdAt(post.getCreatedAt())
                                .updatedAt(post.getUpdatedAt())
                                .build();
                    });

            // when
            DuoPostReadModel result = duoService.createDuoPost(memberId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTier()).isNull();
            assertThat(result.getRank()).isNull();
            assertThat(result.isTierAvailable()).isFalse();
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

            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoPostPersistencePort.save(any(DuoPost.class)))
                    .willReturn(duoPost);

            // when
            duoService.deleteDuoPost(memberId, duoPostId);

            // then
            assertThat(duoPost.getStatus()).isEqualTo(DuoPostStatus.DELETED);
            then(duoPostPersistencePort).should().save(duoPost);
        }
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
            Member member = createTestMember(memberId, true);
            DuoPost duoPost = DuoPost.builder()
                    .id(duoPostId)
                    .memberId(1L)
                    .puuid("owner-puuid")
                    .primaryLane(Lane.MID)
                    .secondaryLane(Lane.JUNGLE)
                    .hasMicrophone(true)
                    .tier("GOLD")
                    .rank("I")
                    .leaguePoints(50)
                    .memo("듀오 구합니다")
                    .status(DuoPostStatus.EXPIRED)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .build();
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .secondaryLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("같이 하실 분")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoService.createDuoRequest(memberId, duoPostId, command))
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
            Member member = createTestMember(memberId, true);
            DuoPost duoPost = createTestDuoPost(duoPostId, memberId);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .secondaryLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("같이 하실 분")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));

            // when & then
            assertThatThrownBy(() -> duoService.createDuoRequest(memberId, duoPostId, command))
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
            Member member = createTestMember(memberId, true);
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .secondaryLane("SUPPORT")
                    .hasMicrophone(false)
                    .memo("같이 하실 분")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.existsByDuoPostIdAndRequesterIdAndStatusIn(
                    eq(duoPostId), eq(memberId), anyList()))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> duoService.createDuoRequest(memberId, duoPostId, command))
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
            Member member = createTestMember(memberId, true);
            DuoPost duoPost = createTestDuoPost(duoPostId, 1L);
            League league = createTestLeague(puuid);
            CreateDuoRequestCommand command = CreateDuoRequestCommand.builder()
                    .primaryLane("ADC")
                    .secondaryLane("SUPPORT")
                    .hasMicrophone(true)
                    .memo("같이 하실 분")
                    .build();

            given(memberPersistencePort.findByIdWithSocialAccounts(memberId))
                    .willReturn(Optional.of(member));
            given(duoPostPersistencePort.findById(duoPostId))
                    .willReturn(Optional.of(duoPost));
            given(duoRequestPersistencePort.existsByDuoPostIdAndRequesterIdAndStatusIn(
                    eq(duoPostId), eq(memberId), anyList()))
                    .willReturn(false);
            given(leaguePersistencePort.findAllLeaguesByPuuid(puuid))
                    .willReturn(List.of(league));
            given(duoRequestPersistencePort.save(any(DuoRequest.class)))
                    .willAnswer(invocation -> {
                        DuoRequest request = invocation.getArgument(0);
                        return DuoRequest.builder()
                                .id(200L)
                                .duoPostId(request.getDuoPostId())
                                .requesterId(request.getRequesterId())
                                .requesterPuuid(request.getRequesterPuuid())
                                .primaryLane(request.getPrimaryLane())
                                .secondaryLane(request.getSecondaryLane())
                                .hasMicrophone(request.isHasMicrophone())
                                .tier(request.getTier())
                                .rank(request.getRank())
                                .leaguePoints(request.getLeaguePoints())
                                .memo(request.getMemo())
                                .status(request.getStatus())
                                .createdAt(request.getCreatedAt())
                                .updatedAt(request.getUpdatedAt())
                                .build();
                    });

            // when
            DuoRequestReadModel result = duoService.createDuoRequest(memberId, duoPostId, command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(200L);
            assertThat(result.getDuoPostId()).isEqualTo(duoPostId);
            assertThat(result.getPrimaryLane()).isEqualTo("ADC");
            assertThat(result.getSecondaryLane()).isEqualTo("SUPPORT");
            assertThat(result.isHasMicrophone()).isTrue();
            assertThat(result.getTier()).isEqualTo("GOLD");
            assertThat(result.getStatus()).isEqualTo("PENDING");
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
            assertThatThrownBy(() -> duoService.acceptDuoRequest(memberId, requestId))
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
            DuoMatchResultReadModel result = duoService.acceptDuoRequest(memberId, requestId);

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
            duoRequest.accept(); // ACCEPTED 상태로 변경

            given(duoRequestPersistencePort.findById(requestId))
                    .willReturn(Optional.of(duoRequest));

            // when & then
            assertThatThrownBy(() -> duoService.confirmDuoRequest(memberId, requestId))
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
            duoRequest.accept(); // ACCEPTED 상태로 변경
            DuoPost duoPost = DuoPost.builder()
                    .id(duoPostId)
                    .memberId(1L)
                    .puuid(ownerPuuid)
                    .primaryLane(Lane.MID)
                    .secondaryLane(Lane.JUNGLE)
                    .hasMicrophone(true)
                    .tier("GOLD")
                    .rank("I")
                    .leaguePoints(50)
                    .memo("듀오 구합니다")
                    .status(DuoPostStatus.ACTIVE)
                    .expiresAt(LocalDateTime.now().plusHours(24))
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
            DuoMatchResultReadModel result = duoService.confirmDuoRequest(requesterId, requestId);

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
            duoService.rejectDuoRequest(memberId, requestId);

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
            duoService.cancelDuoRequest(requesterId, requestId);

            // then
            assertThat(duoRequest.getStatus()).isEqualTo(DuoRequestStatus.CANCELLED);
            then(duoRequestPersistencePort).should().save(duoRequest);
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
            assertThatThrownBy(() -> duoService.getDuoRequestsForPost(memberId, duoPostId))
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
            List<DuoRequestReadModel> result = duoService.getDuoRequestsForPost(memberId, duoPostId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(200L);
            assertThat(result.get(0).getDuoPostId()).isEqualTo(duoPostId);
        }
    }

    // --- 테스트 헬퍼 메서드 ---

    private Member createTestMember(Long id, boolean hasRiot) {
        List<SocialAccount> socialAccounts = new ArrayList<>();
        if (hasRiot) {
            socialAccounts.add(SocialAccount.builder()
                    .id(1L)
                    .memberId(id)
                    .provider("RIOT")
                    .providerId("riot-provider-id")
                    .email("test@riot.com")
                    .nickname("라이엇유저")
                    .puuid("test-puuid")
                    .linkedAt(LocalDateTime.now())
                    .build());
        }
        return Member.builder()
                .id(id)
                .uuid("test-uuid-" + id)
                .nickname("테스터" + id)
                .role("USER")
                .socialAccounts(socialAccounts)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    private League createTestLeague(String puuid) {
        return League.builder()
                .id(1L)
                .puuid(puuid)
                .queue("RANKED_SOLO_5x5")
                .tier("GOLD")
                .rank("I")
                .leaguePoints(50)
                .build();
    }

    private DuoPost createTestDuoPost(Long id, Long memberId) {
        return DuoPost.builder()
                .id(id)
                .memberId(memberId)
                .puuid("owner-puuid")
                .primaryLane(Lane.MID)
                .secondaryLane(Lane.JUNGLE)
                .hasMicrophone(true)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(50)
                .memo("듀오 구합니다")
                .status(DuoPostStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(24))
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
                .secondaryLane(Lane.SUPPORT)
                .hasMicrophone(false)
                .tier("SILVER")
                .rank("II")
                .leaguePoints(30)
                .memo("같이 하실 분")
                .status(DuoRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
