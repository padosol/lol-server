package com.example.lolserver.repository.duo.adapter;

import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.out.DuoRequestPersistencePort;
import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.repository.duo.entity.DuoRequestEntity;
import com.example.lolserver.repository.duo.mapper.DuoRequestMapper;
import com.example.lolserver.repository.duo.repository.DuoRequestJpaRepository;
import com.example.lolserver.support.SliceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DuoRequestPersistenceAdapter implements DuoRequestPersistencePort {

    private static final int PAGE_SIZE = 20;

    private final DuoRequestJpaRepository duoRequestJpaRepository;
    private final DuoRequestMapper duoRequestMapper;

    @Override
    public DuoRequest save(DuoRequest request) {
        DuoRequestEntity entity = duoRequestMapper.toEntity(request);
        DuoRequestEntity saved = duoRequestJpaRepository.save(entity);
        return duoRequestMapper.toDomain(saved);
    }

    @Override
    public Optional<DuoRequest> findById(Long id) {
        return duoRequestJpaRepository.findById(id)
                .map(duoRequestMapper::toDomain);
    }

    @Override
    public List<DuoRequest> findByDuoPostId(Long duoPostId) {
        return duoRequestMapper.toDomainList(
                duoRequestJpaRepository.findByDuoPostId(duoPostId));
    }

    @Override
    public boolean existsByDuoPostIdAndRequesterIdAndStatusIn(Long duoPostId,
            Long requesterId, List<DuoRequestStatus> statuses) {
        List<String> statusStrings = statuses.stream()
                .map(Enum::name)
                .toList();
        return duoRequestJpaRepository.existsByDuoPostIdAndRequesterIdAndStatusIn(
                duoPostId, requesterId, statusStrings);
    }

    @Override
    public void rejectAllPendingAndAccepted(Long duoPostId, Long excludeRequestId) {
        duoRequestJpaRepository.rejectAllPendingAndAccepted(duoPostId, excludeRequestId);
    }

    @Override
    public SliceResult<DuoRequestReadModel> findByRequesterId(Long requesterId, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Slice<DuoRequestEntity> slice = duoRequestJpaRepository
                .findByRequesterIdOrderByCreatedAtDesc(requesterId, pageable);

        return new SliceResult<>(
                slice.getContent().stream()
                        .map(entity -> DuoRequestReadModel.of(
                                duoRequestMapper.toDomain(entity)))
                        .toList(),
                slice.hasNext()
        );
    }
}
