package com.example.lolserver.community.domain;

import com.example.lolserver.community.domain.vo.CategoryLabel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 게시판(카테고리).
 *
 * <p>라벨은 해석된 문자열이 아니라 <b>로케일 맵 그대로</b> 들고 있다. 폴백 규칙
 * (요청 로케일 → ko → code 원문)은 애플리케이션 계층의 판단이고, 영속성 어댑터가
 * 로케일을 미리 골라버리면 그 지식이 어댑터로 새어나간다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Category {

    /** 게시글이 참조하는 식별자이자 클라이언트가 글을 쓸 때 지정하는 값. */
    private final Long id;
    /** 사람이 읽는 안정 코드. 표시·로그용이며 참조에는 쓰지 않는다. */
    private final String code;
    /** 소속 그룹 코드. 조립과 로그 추적용이며 응답 DTO 에서는 중복이라 빠진다. */
    private final String groupCode;
    private final int displayOrder;
    private final boolean active;
    private final boolean writable;
    private final String icon;
    /** locale → 라벨. 비어 있을 수 있다(그 경우 code 가 노출된다). */
    private final Map<String, CategoryLabel> labels;

    /**
     * 요청 로케일 → ko → code 원문 순으로 표시 이름을 고른다.
     */
    public String resolveName(String locale) {
        CategoryLabel label = resolveLabel(locale);
        return label != null ? label.name() : code;
    }

    /**
     * 설명은 폴백 끝에도 없으면 null 이다. code 를 대신 넣지 않는다 —
     * 설명이 없는 게시판이 코드 문자열을 설명으로 달고 나오면 더 나쁘다.
     */
    public String resolveDescription(String locale) {
        CategoryLabel label = resolveLabel(locale);
        return label != null ? label.description() : null;
    }

    private CategoryLabel resolveLabel(String locale) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        CategoryLabel requested = labels.get(locale);
        return requested != null ? requested : labels.get(DEFAULT_LOCALE);
    }

    public static final String DEFAULT_LOCALE = "ko";
}
