package com.example.lolserver.community.application.model.readmodel;

import java.util.List;

/**
 * 라벨이 해석되고 카테고리까지 묶인 그룹. 배열 순서가 곧 화면 순서다.
 *
 * <p>카테고리가 0개인 그룹도 빈 리스트로 내려간다. 프론트는 그때 "준비 중" 을 그린다.
 */
public record BoardGroupReadModel(
        String code,
        String name,
        int order,
        List<CategoryReadModel> categories
) {
}
