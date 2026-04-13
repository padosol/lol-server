package com.example.lolserver.domain.duo.application.port.in;

import com.example.lolserver.domain.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.domain.duo.application.model.DuoPostDetailReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.support.SliceResult;

public interface DuoPostQueryUseCase {
    SliceResult<DuoPostListReadModel> getDuoPosts(DuoPostSearchCommand command);
    DuoPostDetailReadModel getDuoPost(Long duoPostId, Long currentMemberId);
    SliceResult<DuoPostListReadModel> getMyDuoPosts(Long memberId, int page);
}
