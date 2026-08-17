package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.CategoryReadModel;

/**
 * {@code groupCode} 는 넣지 않는다. 이미 그룹 안에 담겨 내려가므로 중복이다.
 */
public record CategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        int order,
        boolean visible,
        boolean writable,
        String icon
) {
    public static CategoryResponse from(CategoryReadModel readModel) {
        return new CategoryResponse(
                readModel.id(),
                readModel.code(),
                readModel.name(),
                readModel.description(),
                readModel.order(),
                readModel.visible(),
                readModel.writable(),
                readModel.icon()
        );
    }
}
