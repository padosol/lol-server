package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent;
import com.example.lolserver.duo.application.model.event.DuoNotificationEvent.DuoNotificationType;
import com.example.lolserver.duo.application.model.resultmodel.DuoMatchResultModel;
import com.example.lolserver.duo.application.model.readmodel.DuoRequestReadModel;
import com.example.lolserver.duo.application.model.resultmodel.DuoRequestResultModel;
import com.example.lolserver.duo.application.port.in.DuoRequestQueryUseCase;
import com.example.lolserver.duo.application.port.in.DuoRequestUseCase;
import com.example.lolserver.duo.application.port.out.DuoLockPort;
import com.example.lolserver.duo.application.port.out.DuoNotificationPort;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.summoner.application.model.readmodel.SummonerReadModel;
import com.example.lolserver.summoner.application.port.in.SummonerQueryUseCase;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuoRequestService implements DuoRequestUseCase, DuoRequestQueryUseCase {

    private static final String DUO_POST_LOCK_PREFIX = "duo:post:";

    private final DuoRequestPersistencePort duoRequestPersistencePort;
    private final DuoPostPersistencePort duoPostPersistencePort;
    private final SummonerQueryUseCase summonerQueryUseCase;
    private final RiotAccountResolver riotAccountResolver;
    private final DuoLockPort duoLockPort;
    private final DuoNotificationPort duoNotificationPort;

    @Override
    @Transactional
    public DuoRequestResultModel createDuoRequest(Long memberId, Long duoPostId,
            CreateDuoRequestCommand command) {
        String puuid = riotAccountResolver.extractRiotPuuid(memberId);

        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateActive();
        duoPost.validateNotOwner(memberId);

        boolean alreadyRequested = duoRequestPersistencePort
                .existsByDuoPostIdAndRequesterIdAndStatusIn(
                        duoPostId, memberId,
                        List.of(DuoRequestStatus.PENDING, DuoRequestStatus.ACCEPTED)
                );
        if (alreadyRequested) {
            throw new CoreException(ErrorType.DUO_REQUEST_ALREADY_EXISTS);
        }

        RiotAccountStats stats = riotAccountResolver.lookupAllStats(puuid);

        DuoRequest duoRequest = DuoRequest.create(
                duoPostId, memberId, puuid,
                Lane.from(command.getPrimaryLane()),
                Lane.from(command.getDesiredLane()),
                command.isHasMicrophone(), command.getMemo(),
                stats
        );

        DuoRequest saved = duoRequestPersistencePort.save(duoRequest);
        return DuoRequestResultModel.of(saved);
    }

    @Override
    @Transactional
    public DuoMatchResultModel acceptDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateOwner(memberId);

        duoRequest.accept();
        duoRequestPersistencePort.save(duoRequest);

        duoNotificationPort.notify(new DuoNotificationEvent(
                DuoNotificationType.REQUEST_ACCEPTED,
                duoRequest.getRequesterId(), duoRequest.getDuoPostId(), requestId));

        return DuoMatchResultModel.of(duoPost, duoRequest);
    }

    @Override
    @Transactional
    public DuoMatchResultModel confirmDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        duoRequest.validateRequester(memberId);

        return duoLockPort.executeWithLock(
                DUO_POST_LOCK_PREFIX + duoRequest.getDuoPostId(), () -> {
                    // 락 획득 후 게시글 상태를 다시 읽어 선착순을 보장한다.
                    // 이미 매칭된 게시글이면 markMatched 가 DUO_POST_NOT_ACTIVE 를 던진다.
                    DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                            .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

                    duoPost.markMatched();

                    // DB 조건부 UPDATE 로 선착순을 원자적으로 확정한다 — 락 해제~커밋
                    // 윈도우에서 미커밋 ACTIVE 를 읽은 경쟁자는 여기서 0건 갱신으로 걸러진다.
                    if (!duoPostPersistencePort.markMatchedIfActive(duoPost.getId())) {
                        throw new CoreException(ErrorType.DUO_POST_NOT_ACTIVE);
                    }

                    duoRequest.confirm();
                    duoRequestPersistencePort.save(duoRequest);

                    // 벌크 close 전에 탈락 대상을 조회해 둔다 (알림 대상 식별용).
                    List<DuoRequest> losingRequests = duoRequestPersistencePort
                            .findByDuoPostId(duoPost.getId()).stream()
                            .filter(request -> !request.getId().equals(duoRequest.getId()))
                            .filter(request -> request.getStatus() == DuoRequestStatus.PENDING
                                    || request.getStatus() == DuoRequestStatus.ACCEPTED)
                            .toList();

                    duoRequestPersistencePort.closeAllOpenExcept(
                            duoPost.getId(), duoRequest.getId());

                    SummonerReadModel partnerSummoner = summonerQueryUseCase
                            .findSummonerByPuuid(duoPost.getPuuid()).orElse(null);

                    duoNotificationPort.notify(new DuoNotificationEvent(
                            DuoNotificationType.MATCH_CONFIRMED,
                            duoPost.getMemberId(), duoPost.getId(), duoRequest.getId()));
                    duoNotificationPort.notify(new DuoNotificationEvent(
                            DuoNotificationType.MATCH_CONFIRMED,
                            duoRequest.getRequesterId(), duoPost.getId(), duoRequest.getId()));
                    losingRequests.forEach(losing -> duoNotificationPort.notify(
                            new DuoNotificationEvent(DuoNotificationType.REQUEST_CLOSED,
                                    losing.getRequesterId(), duoPost.getId(), losing.getId())));

                    return DuoMatchResultModel.of(duoPost, duoRequest, partnerSummoner);
                });
    }

    @Override
    @Transactional
    public void rejectDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateOwner(memberId);

        duoRequest.reject();
        duoRequestPersistencePort.save(duoRequest);
    }

    @Override
    @Transactional
    public void cancelDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        duoRequest.validateRequester(memberId);

        duoRequest.cancel();
        duoRequestPersistencePort.save(duoRequest);
    }

    @Override
    public List<DuoRequestReadModel> getDuoRequestsForPost(Long memberId, Long duoPostId) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateOwner(memberId);

        return duoRequestPersistencePort.findByDuoPostId(duoPostId).stream()
                .map(DuoRequestReadModel::of)
                .toList();
    }

    @Override
    public SliceResult<DuoRequestReadModel> getMyDuoRequests(Long memberId, int page) {
        return duoRequestPersistencePort.findByRequesterId(memberId, page);
    }

    @Override
    public DuoMatchResultModel getMatchResult(Long memberId, Long duoPostId) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateMatched();

        DuoRequest confirmedRequest = duoRequestPersistencePort
                .findConfirmedByDuoPostId(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        String partnerPuuid;
        if (duoPost.isOwner(memberId)) {
            partnerPuuid = confirmedRequest.getRequesterPuuid();
        } else if (confirmedRequest.getRequesterId().equals(memberId)) {
            partnerPuuid = duoPost.getPuuid();
        } else {
            throw new CoreException(ErrorType.FORBIDDEN);
        }

        SummonerReadModel partnerSummoner = summonerQueryUseCase
                .findSummonerByPuuid(partnerPuuid).orElse(null);

        return DuoMatchResultModel.of(duoPost, confirmedRequest, partnerSummoner);
    }
}
