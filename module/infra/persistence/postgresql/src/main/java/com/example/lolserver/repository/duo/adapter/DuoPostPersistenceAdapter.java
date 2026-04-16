package com.example.lolserver.repository.duo.adapter;

import com.example.lolserver.domain.duo.application.command.DuoPostSearchCommand;
import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.domain.duo.application.port.out.DuoPostPersistencePort;
import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.repository.duo.dto.DuoPostListDTO;
import com.example.lolserver.repository.duo.dsl.DuoPostRepositoryCustom;
import com.example.lolserver.repository.duo.entity.DuoPostEntity;
import com.example.lolserver.repository.duo.mapper.DuoPostMapper;
import com.example.lolserver.repository.duo.repository.DuoPostJpaRepository;
import com.example.lolserver.support.SliceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DuoPostPersistenceAdapter implements DuoPostPersistencePort {

    private static final int PAGE_SIZE = 20;

    private final DuoPostJpaRepository duoPostJpaRepository;
    private final DuoPostRepositoryCustom duoPostRepositoryCustom;
    private final DuoPostMapper duoPostMapper;

    @Override
    public DuoPost save(DuoPost duoPost) {
        DuoPostEntity entity = duoPostMapper.toEntity(duoPost);
        DuoPostEntity saved = duoPostJpaRepository.save(entity);
        return duoPostMapper.toDomain(saved);
    }

    @Override
    public Optional<DuoPost> findById(Long id) {
        return duoPostJpaRepository.findById(id)
                .map(duoPostMapper::toDomain);
    }

    @Override
    public SliceResult<DuoPostListReadModel> findActivePosts(DuoPostSearchCommand command) {
        Pageable pageable = PageRequest.of(command.getPage(), PAGE_SIZE);

        Slice<DuoPostListDTO> slice = duoPostRepositoryCustom.findActivePosts(
                command.getLane(), command.getTier(), pageable);

        return toSliceResult(slice);
    }

    @Override
    public SliceResult<DuoPostListReadModel> findByMemberId(Long memberId, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Slice<DuoPostListDTO> slice = duoPostRepositoryCustom.findByMemberId(memberId, pageable);

        return toSliceResult(slice);
    }

    private SliceResult<DuoPostListReadModel> toSliceResult(Slice<DuoPostListDTO> slice) {
        return new SliceResult<>(
                slice.getContent().stream()
                        .map(dto -> DuoPostListReadModel.builder()
                                .id(dto.getId())
                                .primaryLane(dto.getPrimaryLane())
                                .desiredLane(dto.getDesiredLane())
                                .hasMicrophone(dto.isHasMicrophone())
                                .tier(dto.getTier())
                                .rank(dto.getRank())
                                .leaguePoints(dto.getLeaguePoints())
                                .memo(dto.getMemo())
                                .status(dto.getStatus())
                                .requestCount((int) dto.getRequestCount())
                                .mostChampions(dto.getMostChampions())
                                .recentGameSummary(dto.getRecentGameSummary())
                                .expiresAt(dto.getExpiresAt())
                                .createdAt(dto.getCreatedAt())
                                .build())
                        .toList(),
                slice.hasNext()
        );
    }
}
