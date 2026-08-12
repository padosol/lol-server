package com.example.lolserver.community.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 사이드바 게시판 그룹(섹션).
 *
 * <p>카테고리와 한 테이블/한 타입으로 합치지 않는다. 그룹에는 글이 붙지 않고
 * 계층이 2단으로 고정이라 재귀 구조가 필요 없다.
 *
 * <p>{@code categories} 는 이미 정렬된 상태로 채워진다(§3-3 정렬 계약). 라벨을
 * 로케일 맵으로 들고 있는 이유는 {@link Category} 와 같다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardGroup {

    private final String code;
    private final int displayOrder;
    private final boolean active;
    /** locale → 표시 이름. 비어 있을 수 있다(그 경우 code 가 노출된다). */
    private final Map<String, String> labels;
    private final List<Category> categories;

    /**
     * 요청 로케일 → ko → code 원문 순으로 표시 이름을 고른다.
     */
    public String resolveName(String locale) {
        if (labels == null || labels.isEmpty()) {
            return code;
        }
        String requested = labels.get(locale);
        if (requested != null) {
            return requested;
        }
        return labels.getOrDefault(Category.DEFAULT_LOCALE, code);
    }
}
