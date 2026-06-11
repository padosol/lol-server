package com.example.lolserver.duo.application.port.out;

import com.example.lolserver.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.duo.application.model.readmodel.DuoPostListReadModel;
import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.common.support.SliceResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DuoPostPersistencePort {
    DuoPost save(DuoPost duoPost);
    Optional<DuoPost> findById(Long id);
    boolean existsActiveByMemberId(Long memberId);
    SliceResult<DuoPostListReadModel> findActivePosts(DuoPostSearchCommand command);
    SliceResult<DuoPostListReadModel> findByMemberId(Long memberId, int page);
    List<Long> expireAllOverdue(LocalDateTime now);
}
