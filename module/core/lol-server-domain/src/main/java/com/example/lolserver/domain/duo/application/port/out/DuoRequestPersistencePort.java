package com.example.lolserver.domain.duo.application.port.out;

import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.support.SliceResult;

import java.util.List;
import java.util.Optional;

public interface DuoRequestPersistencePort {
    DuoRequest save(DuoRequest request);
    Optional<DuoRequest> findById(Long id);
    List<DuoRequest> findByDuoPostId(Long duoPostId);
    boolean existsByDuoPostIdAndRequesterIdAndStatusIn(Long duoPostId,
            Long requesterId, List<DuoRequestStatus> statuses);
    void rejectAllPendingAndAccepted(Long duoPostId, Long excludeRequestId);
    SliceResult<DuoRequestReadModel> findByRequesterId(Long requesterId, int page);
}
