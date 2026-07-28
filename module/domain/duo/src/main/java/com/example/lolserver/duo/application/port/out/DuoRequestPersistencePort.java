package com.example.lolserver.duo.application.port.out;

import com.example.lolserver.duo.application.model.readmodel.DuoRequestReadModel;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.common.support.SliceResult;

import java.util.List;
import java.util.Optional;

public interface DuoRequestPersistencePort {
    DuoRequest save(DuoRequest request);
    Optional<DuoRequest> findById(Long id);
    List<DuoRequest> findByDuoPostId(Long duoPostId);
    Optional<DuoRequest> findConfirmedByDuoPostId(Long duoPostId);
    boolean existsByDuoPostIdAndRequesterIdAndStatusIn(Long duoPostId,
            Long requesterId, List<DuoRequestStatus> statuses);
    void closeAllOpenExcept(Long duoPostId, Long excludeRequestId);
    void closeAllOpen(Long duoPostId);
    SliceResult<DuoRequestReadModel> findByRequesterId(Long requesterId, int page);
}
