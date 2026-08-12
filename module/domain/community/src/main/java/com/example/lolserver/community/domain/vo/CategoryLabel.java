package com.example.lolserver.community.domain.vo;

/**
 * 카테고리의 로케일별 표시 라벨.
 *
 * <p>description 은 없을 수 있다(현재 시드는 name 만 채운다).
 */
public record CategoryLabel(String name, String description) {

    public static CategoryLabel of(String name, String description) {
        return new CategoryLabel(name, description);
    }
}
