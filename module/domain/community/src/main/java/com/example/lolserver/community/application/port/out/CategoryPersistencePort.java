package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.domain.BoardGroup;
import com.example.lolserver.community.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryPersistencePort {

    /**
     * 활성 그룹에 카테고리를 묶어 <b>정렬된</b> 상태로 반환한다.
     * 정렬 계약: 그룹은 (display_order, code), 그룹 안의 카테고리도 (display_order, code).
     *
     * <p>비활성 카테고리도 포함된다. 숨김 여부는 호출 측이 {@code active} 로 판단하며,
     * 라벨 해석에는 비활성 카테고리도 필요하다(기존 글의 배지).
     */
    List<BoardGroup> findAllGroupsWithCategories();

    Optional<Category> findCategoryById(Long id);
}
