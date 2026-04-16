package com.example.lolserver.domain.duo.application.port.in;

import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.support.SliceResult;

import java.util.List;

public interface DuoRequestQueryUseCase {
    List<DuoRequestReadModel> getDuoRequestsForPost(Long memberId, Long duoPostId);
    SliceResult<DuoRequestReadModel> getMyDuoRequests(Long memberId, int page);
}
