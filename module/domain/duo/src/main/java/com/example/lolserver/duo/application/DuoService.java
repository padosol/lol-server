package com.example.lolserver.duo.application;

import com.example.lolserver.duo.application.command.CreateDuoPostCommand;
import com.example.lolserver.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.duo.application.command.UpdateDuoPostCommand;
import com.example.lolserver.duo.application.model.readmodel.DuoPostDetailReadModel;
import com.example.lolserver.duo.application.model.readmodel.DuoPostListReadModel;
import com.example.lolserver.duo.application.model.resultmodel.DuoPostResultModel;
import com.example.lolserver.duo.application.model.readmodel.DuoRequestReadModel;
import com.example.lolserver.duo.application.port.in.DuoPostQueryUseCase;
import com.example.lolserver.duo.application.port.in.DuoPostUseCase;
import com.example.lolserver.duo.application.port.out.DuoLockPort;
import com.example.lolserver.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.vo.Lane;
import com.example.lolserver.duo.domain.vo.RiotAccountStats;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
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
public class DuoService implements DuoPostUseCase, DuoPostQueryUseCase {

    private static final String MEMBER_POST_LOCK_PREFIX = "duo:member:post:";

    private final DuoPostPersistencePort duoPostPersistencePort;
    private final DuoRequestPersistencePort duoRequestPersistencePort;
    private final RiotAccountResolver riotAccountResolver;
    private final DuoLockPort duoLockPort;

    @Override
    @Transactional
    public DuoPostResultModel createDuoPost(Long memberId, CreateDuoPostCommand command) {
        String puuid = riotAccountResolver.extractRiotPuuid(memberId);

        // 락 안에서 활성글 존재 검사와 저장을 함께 수행해 check-then-act 레이스를 제거한다.
        return duoLockPort.executeWithLock(MEMBER_POST_LOCK_PREFIX + memberId, () -> {
            if (duoPostPersistencePort.existsActiveByMemberId(memberId)) {
                throw new CoreException(ErrorType.DUO_POST_ACTIVE_EXISTS);
            }

            RiotAccountStats stats = riotAccountResolver.lookupAllStats(puuid);

            DuoPost duoPost = DuoPost.create(
                    memberId, puuid,
                    Lane.from(command.getPrimaryLane()),
                    Lane.from(command.getDesiredLane()),
                    command.isHasMicrophone(), command.getMemo(),
                    stats
            );

            DuoPost saved = duoPostPersistencePort.save(duoPost);
            return DuoPostResultModel.of(saved);
        });
    }

    @Override
    @Transactional
    public void deleteDuoPost(Long memberId, Long duoPostId) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateOwner(memberId);

        duoPost.markDeleted();
        duoPostPersistencePort.save(duoPost);

        duoRequestPersistencePort.closeAllOpen(duoPostId);
    }

    @Override
    @Transactional
    public DuoPostResultModel updateDuoPost(Long memberId, Long duoPostId,
                                           UpdateDuoPostCommand command) {
        DuoPost duoPost = duoPostPersistencePort.findById(duoPostId)
                .orElseThrow(() -> new CoreException(ErrorType.DUO_POST_NOT_FOUND));

        duoPost.validateOwner(memberId);
        duoPost.validateActive();

        duoPost.updateContent(
                command.getPrimaryLane(), command.getDesiredLane(),
                command.isHasMicrophone(), command.getMemo());

        DuoPost saved = duoPostPersistencePort.save(duoPost);
        return DuoPostResultModel.of(saved);
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
}
