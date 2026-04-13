package com.example.lolserver.domain.duo.application;

import com.example.lolserver.domain.duo.application.command.CreateDuoPostCommand;
import com.example.lolserver.domain.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.domain.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.domain.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostDetailReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.in.DuoPostQueryUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoPostUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoRequestQueryUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoRequestUseCase;
import com.example.lolserver.domain.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.domain.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.domain.duo.domain.vo.TierInfo;
import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import com.example.lolserver.domain.member.application.port.out.MemberPersistencePort;
import com.example.lolserver.domain.member.domain.Member;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.QueueType;
import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.domain.Summoner;
import com.example.lolserver.support.SliceResult;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuoService implements DuoPostUseCase, DuoPostQueryUseCase,
        DuoRequestUseCase, DuoRequestQueryUseCase {

    private final DuoPostPersistencePort duoPostPersistencePort;
    private final DuoRequestPersistencePort duoRequestPersistencePort;
    private final MemberPersistencePort memberPersistencePort;
    private final LeaguePersistencePort leaguePersistencePort;
    private final SummonerPersistencePort summonerPersistencePort;

    @Override
    @Transactional
    public DuoPostReadModel createDuoPost(Long memberId, CreateDuoPostCommand command) {
        String puuid = extractRiotPuuid(memberId);
        TierInfo tierInfo = lookupTierInfo(puuid);

        Lane primaryLane = parseLane(command.getPrimaryLane());
        Lane secondaryLane = parseLane(command.getSecondaryLane());

        DuoPost duoPost = DuoPost.create(
                memberId, puuid, primaryLane, secondaryLane,
                command.isHasMicrophone(), tierInfo, command.getMemo()
        );

        DuoPost saved = duoPostPersistencePort.save(duoPost);
        return DuoPostReadModel.of(saved);
    }

    @Override
    @Transactional
    public void deleteDuoPost(Long memberId, Long duoPostId) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        if (!duoPost.isOwner(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        duoPost.markDeleted();
        duoPostPersistencePort.save(duoPost);
    }

    @Override
    public SliceResult<DuoPostListReadModel> getDuoPosts(DuoPostSearchCommand command) {
        return duoPostPersistencePort.findActivePosts(command);
    }

    @Override
    public DuoPostDetailReadModel getDuoPost(Long duoPostId, Long currentMemberId) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        boolean isOwner = currentMemberId != null && duoPost.isOwner(currentMemberId);

        List<DuoRequestReadModel> requests;
        if (isOwner) {
            requests = duoRequestPersistencePort.findByDuoPostId(duoPostId).stream()
                    .map(DuoRequestReadModel::of)
                    .toList();
        } else {
            requests = Collections.emptyList();
        }

        return DuoPostDetailReadModel.of(duoPost, isOwner, requests);
    }

    @Override
    public SliceResult<DuoPostListReadModel> getMyDuoPosts(Long memberId, int page) {
        return duoPostPersistencePort.findByMemberId(memberId, page);
    }

    @Override
    @Transactional
    public DuoRequestReadModel createDuoRequest(Long memberId, Long duoPostId,
            CreateDuoRequestCommand command) {
        String puuid = extractRiotPuuid(memberId);

        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        if (!duoPost.isActive()) {
            throw new CoreException(ErrorType.DUO_POST_NOT_ACTIVE);
        }

        if (duoPost.isOwner(memberId)) {
            throw new CoreException(ErrorType.DUO_POST_SELF_REQUEST);
        }

        boolean alreadyRequested = duoRequestPersistencePort
                .existsByDuoPostIdAndRequesterIdAndStatusIn(
                        duoPostId, memberId,
                        List.of(DuoRequestStatus.PENDING, DuoRequestStatus.ACCEPTED)
                );
        if (alreadyRequested) {
            throw new CoreException(ErrorType.DUO_REQUEST_ALREADY_EXISTS);
        }

        TierInfo tierInfo = lookupTierInfo(puuid);

        Lane primaryLane = parseLane(command.getPrimaryLane());
        Lane secondaryLane = parseLane(command.getSecondaryLane());

        DuoRequest duoRequest = DuoRequest.create(
                duoPostId, memberId, puuid, primaryLane, secondaryLane,
                command.isHasMicrophone(), tierInfo, command.getMemo()
        );

        DuoRequest saved = duoRequestPersistencePort.save(duoRequest);
        return DuoRequestReadModel.of(saved);
    }

    @Override
    @Transactional
    public DuoMatchResultReadModel acceptDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        if (!duoPost.isOwner(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        duoRequest.accept();
        duoRequestPersistencePort.save(duoRequest);

        return DuoMatchResultReadModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(null)
                .partnerTagLine(null)
                .status(DuoRequestStatus.ACCEPTED.name())
                .build();
    }

    @Override
    @Transactional
    public DuoMatchResultReadModel confirmDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        if (!duoRequest.isRequester(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        duoRequest.confirm();
        duoRequestPersistencePort.save(duoRequest);

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.markMatched();
        duoPostPersistencePort.save(duoPost);

        duoRequestPersistencePort.rejectAllPendingAndAccepted(
                duoPost.getId(), duoRequest.getId());

        String partnerGameName = null;
        String partnerTagLine = null;
        Summoner partnerSummoner = summonerPersistencePort
                .findById(duoPost.getPuuid()).orElse(null);
        if (partnerSummoner != null) {
            partnerGameName = partnerSummoner.getGameName();
            partnerTagLine = partnerSummoner.getTagLine();
        }

        return DuoMatchResultReadModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(partnerGameName)
                .partnerTagLine(partnerTagLine)
                .status(DuoRequestStatus.CONFIRMED.name())
                .build();
    }

    @Override
    @Transactional
    public void rejectDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        if (!duoPost.isOwner(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        duoRequest.reject();
        duoRequestPersistencePort.save(duoRequest);
    }

    @Override
    @Transactional
    public void cancelDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        if (!duoRequest.isRequester(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        duoRequest.cancel();
        duoRequestPersistencePort.save(duoRequest);
    }

    @Override
    public List<DuoRequestReadModel> getDuoRequestsForPost(Long memberId, Long duoPostId) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        if (!duoPost.isOwner(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        return duoRequestPersistencePort.findByDuoPostId(duoPostId).stream()
                .map(DuoRequestReadModel::of)
                .toList();
    }

    @Override
    public SliceResult<DuoRequestReadModel> getMyDuoRequests(Long memberId, int page) {
        return duoRequestPersistencePort.findByRequesterId(memberId, page);
    }

    private String extractRiotPuuid(Long memberId) {
        Member member = memberPersistencePort.findByIdWithSocialAccounts(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        return member.getSocialAccounts().stream()
                .filter(sa -> OAuthProvider.RIOT.name().equals(sa.getProvider()) && sa.getPuuid() != null)
                .map(sa -> sa.getPuuid())
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.RIOT_ACCOUNT_NOT_LINKED));
    }

    private TierInfo lookupTierInfo(String puuid) {
        return leaguePersistencePort.findAllLeaguesByPuuid(puuid).stream()
                .filter(league -> QueueType.RANKED_SOLO_5x5.name().equals(league.getQueue()))
                .findFirst()
                .map(league -> new TierInfo(
                        league.getTier(), league.getRank(), league.getLeaguePoints()))
                .orElse(TierInfo.UNRANKED);
    }

    private Lane parseLane(String lane) {
        try {
            return Lane.valueOf(lane);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_LANE);
        }
    }
}
