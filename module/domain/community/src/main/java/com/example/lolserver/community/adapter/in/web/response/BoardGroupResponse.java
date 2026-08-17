package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.BoardGroupReadModel;

import java.util.List;

public record BoardGroupResponse(
        String code,
        String name,
        int order,
        List<CategoryResponse> categories
) {
    public static BoardGroupResponse from(BoardGroupReadModel readModel) {
        return new BoardGroupResponse(
                readModel.code(),
                readModel.name(),
                readModel.order(),
                readModel.categories().stream().map(CategoryResponse::from).toList()
        );
    }
}
