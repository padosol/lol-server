package com.example.lolserver.duo.application.port.in;

import com.example.lolserver.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.duo.application.model.readmodel.DuoPostDetailReadModel;
import com.example.lolserver.duo.application.model.readmodel.DuoPostListReadModel;
import com.example.lolserver.common.support.SliceResult;

public interface DuoPostQueryUseCase {
    SliceResult<DuoPostListReadModel> getDuoPosts(DuoPostSearchCommand command);
    DuoPostDetailReadModel getDuoPost(Long duoPostId, Long currentMemberId);
    SliceResult<DuoPostListReadModel> getMyDuoPosts(Long memberId, int page);
}
