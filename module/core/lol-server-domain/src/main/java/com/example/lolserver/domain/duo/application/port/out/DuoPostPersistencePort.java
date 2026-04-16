package com.example.lolserver.domain.duo.application.port.out;

import com.example.lolserver.domain.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.support.SliceResult;

import java.util.Optional;

public interface DuoPostPersistencePort {
    DuoPost save(DuoPost duoPost);
    Optional<DuoPost> findById(Long id);
    SliceResult<DuoPostListReadModel> findActivePosts(DuoPostSearchCommand command);
    SliceResult<DuoPostListReadModel> findByMemberId(Long memberId, int page);
}
