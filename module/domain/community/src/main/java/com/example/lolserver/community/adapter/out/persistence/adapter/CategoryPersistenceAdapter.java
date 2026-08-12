package com.example.lolserver.community.adapter.out.persistence.adapter;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBoardGroupEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryI18nEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBoardGroupI18nJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBoardGroupJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityCategoryI18nJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityCategoryJpaRepository;
import com.example.lolserver.community.application.port.out.CategoryPersistencePort;
import com.example.lolserver.community.domain.BoardGroup;
import com.example.lolserver.community.domain.Category;
import com.example.lolserver.community.domain.vo.CategoryLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 그룹–카테고리 조립을 담당한다.
 *
 * <p>JPA 연관관계로 지연 로딩을 거는 대신 각 테이블을 {@code ORDER BY} 로 한 번씩
 * 조회한 뒤 메모리에서 {@code groupId} 기준으로 묶는다. 행이 수십 건이라 N+1 도 조인
 * 중복도 신경 쓸 규모가 아니고, 정렬을 SQL 에 맡길 수 있어 정렬 계약이 쿼리 메서드
 * 이름 한 곳에 모인다.
 *
 * <p>라벨은 로케일을 고르지 않고 통째로 실어 보낸다. 폴백 규칙(요청 로케일 → ko →
 * code)은 도메인/애플리케이션의 판단이라 여기서 미리 좁히면 그 지식이 어댑터로 샌다.
 */
@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryPersistencePort {

    private final CommunityBoardGroupJpaRepository boardGroupRepository;
    private final CommunityCategoryJpaRepository categoryRepository;
    private final CommunityBoardGroupI18nJpaRepository boardGroupI18nRepository;
    private final CommunityCategoryI18nJpaRepository categoryI18nRepository;

    /**
     * 비활성 <b>그룹</b>은 통째로 제외한다. 응답 트리에 그룹의 노출 플래그가 없기
     * 때문이며, 그룹을 접는다는 것은 그 안의 게시판을 다 접는다는 뜻이다. 게시판
     * 하나만 숨기려면 카테고리의 active 를 쓴다(그쪽은 라벨 해석을 위해 응답에 남는다).
     */
    @Override
    public List<BoardGroup> findAllGroupsWithCategories() {
        List<CommunityBoardGroupEntity> groups =
                boardGroupRepository.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
        if (groups.isEmpty()) {
            return List.of();
        }

        Map<Long, String> groupCodeById = new HashMap<>();
        for (CommunityBoardGroupEntity group : groups) {
            groupCodeById.put(group.getId(), group.getCode());
        }

        Map<Long, Map<String, String>> groupLabels = groupLabelsByGroupId();
        Map<Long, Map<String, CategoryLabel>> categoryLabels = categoryLabelsByCategoryId();

        // 카테고리는 정렬된 순서로 읽어 그룹별 리스트에 차례로 넣는다. LinkedHashMap 이
        // 아니라 그룹 순회 순서로 꺼내므로 그룹 정렬은 위 쿼리가 이미 보장한다.
        Map<Long, List<Category>> categoriesByGroupId = new LinkedHashMap<>();
        for (CommunityCategoryEntity entity : categoryRepository.findAllByOrderByDisplayOrderAscCodeAsc()) {
            String groupCode = groupCodeById.get(entity.getGroupId());
            if (groupCode == null) {
                // 비활성 그룹에 속한 카테고리. 그룹이 응답에 없으므로 함께 빠진다.
                continue;
            }
            categoriesByGroupId
                    .computeIfAbsent(entity.getGroupId(), key -> new ArrayList<>())
                    .add(toDomain(entity, groupCode, categoryLabels));
        }

        List<BoardGroup> result = new ArrayList<>(groups.size());
        for (CommunityBoardGroupEntity group : groups) {
            result.add(BoardGroup.builder()
                    .code(group.getCode())
                    .displayOrder(group.getDisplayOrder())
                    .active(group.isActive())
                    .labels(groupLabels.getOrDefault(group.getId(), Map.of()))
                    .categories(categoriesByGroupId.getOrDefault(group.getId(), List.of()))
                    .build());
        }
        return result;
    }

    /**
     * 검증 경로라 라벨을 붙이지 않는다. 표시할 일이 없다.
     */
    @Override
    public Optional<Category> findCategoryByCode(String code) {
        return categoryRepository.findByCode(code)
                .map(entity -> toDomain(entity, null, Map.of()));
    }

    private Category toDomain(
            CommunityCategoryEntity entity,
            String groupCode,
            Map<Long, Map<String, CategoryLabel>> labelsByCategoryId) {
        return Category.builder()
                .code(entity.getCode())
                .groupCode(groupCode)
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .writable(entity.isWritable())
                .icon(entity.getIcon())
                .labels(labelsByCategoryId.getOrDefault(entity.getId(), Map.of()))
                .build();
    }

    private Map<Long, Map<String, String>> groupLabelsByGroupId() {
        Map<Long, Map<String, String>> labels = new HashMap<>();
        boardGroupI18nRepository.findAll().forEach(entity ->
                labels.computeIfAbsent(entity.getGroupId(), key -> new HashMap<>())
                        .put(entity.getLocale(), entity.getName()));
        return labels;
    }

    private Map<Long, Map<String, CategoryLabel>> categoryLabelsByCategoryId() {
        Map<Long, Map<String, CategoryLabel>> labels = new HashMap<>();
        for (CommunityCategoryI18nEntity entity : categoryI18nRepository.findAll()) {
            labels.computeIfAbsent(entity.getCategoryId(), key -> new HashMap<>())
                    .put(entity.getLocale(), CategoryLabel.of(entity.getName(), entity.getDescription()));
        }
        return labels;
    }
}
