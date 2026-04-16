package com.example.lolserver.controller.community.response;

import com.example.lolserver.domain.community.application.model.AuthorReadModel;

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
