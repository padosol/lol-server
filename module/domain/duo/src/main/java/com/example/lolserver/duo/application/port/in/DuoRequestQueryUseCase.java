package com.example.lolserver.duo.application.port.in;

import com.example.lolserver.duo.application.model.readmodel.DuoRequestReadModel;
import com.example.lolserver.common.support.SliceResult;

import java.util.List;

public interface DuoRequestQueryUseCase {
    List<DuoRequestReadModel> getDuoRequestsForPost(Long memberId, Long duoPostId);
    SliceResult<DuoRequestReadModel> getMyDuoRequests(Long memberId, int page);
}
