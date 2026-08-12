package com.example.lolserver.community.application.model.readmodel;

/**
 * 라벨이 이미 해석된 카테고리. {@code visible} 은 도메인의 {@code active} 를
 * 화면 관점 이름으로 바꾼 것이다 — 목록/사이드바에 그릴지 여부.
 */
public record CategoryReadModel(
        String code,
        String name,
        String description,
        int order,
        boolean visible,
        boolean writable,
        String icon
) {
}
