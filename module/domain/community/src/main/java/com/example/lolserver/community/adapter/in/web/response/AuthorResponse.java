package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.AuthorReadModel;

public record AuthorResponse(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static AuthorResponse from(AuthorReadModel readModel) {
        if (readModel == null) {
            return null;
        }
        return new AuthorResponse(readModel.id(), readModel.nickname(), readModel.profileImageUrl());
    }
}
