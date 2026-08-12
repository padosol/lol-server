package com.example.lolserver.community.application.port.in;

import com.example.lolserver.community.application.model.readmodel.CategoryTreeReadModel;

public interface CategoryQueryUseCase {

    /**
     * 그룹핑·정렬·라벨 해석이 끝난 카테고리 트리를 반환한다.
     *
     * @param locale 표시 로케일. null 이거나 해당 라벨이 없으면 ko 로 폴백한다.
     */
    CategoryTreeReadModel getCategoryTree(String locale);

    /**
     * 글 작성/수정이 가능한 카테고리인지 검증한다. 존재하지 않거나 숨김이거나
     * 읽기 전용이면 예외를 던진다.
     */
    void validateWritable(String categoryCode);
}
