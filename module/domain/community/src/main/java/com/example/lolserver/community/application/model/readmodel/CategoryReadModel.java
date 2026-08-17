package com.example.lolserver.community.application.model.readmodel;

/**
 * 라벨이 이미 해석된 카테고리. {@code visible} 은 도메인의 {@code active} 를
 * 화면 관점 이름으로 바꾼 것이다 — 목록/사이드바에 그릴지 여부.
 *
 * <p>{@code id} 는 클라이언트가 글을 쓰거나 목록을 필터할 때 그대로 되돌려 보내는 값이고,
 * {@code code} 는 표시·디버깅용으로 함께 내려간다.
 */
public record CategoryReadModel(
        Long id,
        String code,
        String name,
        String description,
        int order,
        boolean visible,
        boolean writable,
        String icon
) {
}
