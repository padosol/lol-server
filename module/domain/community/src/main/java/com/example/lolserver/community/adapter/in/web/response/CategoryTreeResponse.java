package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.CategoryTreeReadModel;

import java.util.List;

/**
 * 배열 순서가 곧 화면 순서다. 프론트는 groups 를 순회하며 각 그룹의 categories 를
 * 그리기만 하면 되고 정렬도 그룹핑도 하지 않는다.
 */
public record CategoryTreeResponse(List<BoardGroupResponse> groups) {

    public static CategoryTreeResponse from(CategoryTreeReadModel readModel) {
        return new CategoryTreeResponse(
                readModel.groups().stream().map(BoardGroupResponse::from).toList()
        );
    }
}
