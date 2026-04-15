package com.example.lolserver.repository.duo.dsl;

import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.repository.duo.dto.DuoPostListDTO;
import com.example.lolserver.repository.duo.entity.DuoPostEntity;
import com.example.lolserver.repository.duo.repository.DuoPostJpaRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.lolserver.repository.duo.entity.QDuoPostEntity.duoPostEntity;
import static com.example.lolserver.repository.duo.entity.QDuoRequestEntity.duoRequestEntity;

@Repository
@RequiredArgsConstructor
public class DuoPostRepositoryCustomImpl implements DuoPostRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final DuoPostJpaRepository duoPostJpaRepository;

    @Override
    public Slice<DuoPostListDTO> findActivePosts(String lane, String tier, Pageable pageable) {
        return queryDuoPosts(pageable,
                duoPostEntity.status.eq(DuoPostStatus.ACTIVE.name()),
                duoPostEntity.expiresAt.after(LocalDateTime.now()),
                laneEq(lane),
                tierEq(tier)
        );
    }

    @Override
    public Slice<DuoPostListDTO> findByMemberId(Long memberId, Pageable pageable) {
        return queryDuoPosts(pageable,
                duoPostEntity.memberId.eq(memberId)
        );
    }

    private Slice<DuoPostListDTO> queryDuoPosts(Pageable pageable,
            BooleanExpression... conditions) {
        int pageSize = pageable.getPageSize();

        List<DuoPostListDTO> result = jpaQueryFactory
                .select(Projections.constructor(DuoPostListDTO.class,
                        duoPostEntity.id,
                        duoPostEntity.primaryLane,
                        duoPostEntity.desiredLane,
                        duoPostEntity.hasMicrophone,
                        duoPostEntity.tier,
                        duoPostEntity.tierRank,
                        duoPostEntity.leaguePoints,
                        duoPostEntity.memo,
                        duoPostEntity.status,
                        duoRequestEntity.id.count(),
                        duoPostEntity.expiresAt,
                        duoPostEntity.createdAt
                ))
                .from(duoPostEntity)
                .leftJoin(duoRequestEntity).on(duoRequestEntity.duoPostId.eq(duoPostEntity.id))
                .where(conditions)
                .groupBy(duoPostEntity.id)
                .orderBy(duoPostEntity.createdAt.desc())
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<DuoPostListDTO> content = hasNext ? result.subList(0, pageSize) : result;

        enrichWithJsonFields(content);

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private void enrichWithJsonFields(List<DuoPostListDTO> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream().map(DuoPostListDTO::getId).toList();
        Map<Long, DuoPostEntity> entityMap = duoPostJpaRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(DuoPostEntity::getId, Function.identity()));

        dtos.forEach(dto -> {
            DuoPostEntity entity = entityMap.get(dto.getId());
            if (entity != null) {
                dto.setMostChampions(entity.getMostChampions());
                dto.setRecentGameSummary(entity.getRecentGameSummary());
            }
        });
    }

    private BooleanExpression laneEq(String lane) {
        return StringUtils.hasText(lane) ? duoPostEntity.primaryLane.eq(lane) : null;
    }

    private BooleanExpression tierEq(String tier) {
        return StringUtils.hasText(tier) ? duoPostEntity.tier.eq(tier) : null;
    }
}
