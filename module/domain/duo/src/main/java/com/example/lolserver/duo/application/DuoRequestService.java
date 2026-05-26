package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.command.CreateDuoRequestCommand;
import com.example.lolserver.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.duo.application.port.in.DuoRequestQueryUseCase;
import com.example.lolserver.duo.application.port.in.DuoRequestUseCase;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.summoner.application.model.SummonerReadModel;
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

    private final DuoRequestPersistencePort duoRequestPersistencePort;
    private final DuoPostPersistencePort duoPostPersistencePort;
    private final SummonerQueryUseCase summonerQueryUseCase;
    private final RiotAccountResolver riotAccountResolver;

    @Override
    @Transactional
    public DuoRequestReadModel createDuoRequest(Long memberId, Long duoPostId,
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
        return DuoRequestReadModel.of(saved);
    }

    @Override
    @Transactional
    public DuoMatchResultReadModel acceptDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateOwner(memberId);

        duoRequest.accept();
        duoRequestPersistencePort.save(duoRequest);

        return DuoMatchResultReadModel.of(duoPost, duoRequest);
    }

    @Override
    @Transactional
    public DuoMatchResultReadModel confirmDuoRequest(Long memberId, Long requestId) {
        DuoRequest duoRequest = duoRequestPersistencePort.findById(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_REQUEST_NOT_FOUND));

        duoRequest.validateRequester(memberId);

        duoRequest.confirm();
        duoRequestPersistencePort.save(duoRequest);

        DuoPost duoPost = duoPostPersistencePort.findById(duoRequest.getDuoPostId())
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.markMatched();
        duoPostPersistencePort.save(duoPost);

        duoRequestPersistencePort.rejectAllPendingAndAccepted(
                duoPost.getId(), duoRequest.getId());

        SummonerReadModel partnerSummoner = summonerQueryUseCase
                .findSummonerByPuuid(duoPost.getPuuid()).orElse(null);

        return DuoMatchResultReadModel.of(duoPost, duoRequest, partnerSummoner);
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
}
