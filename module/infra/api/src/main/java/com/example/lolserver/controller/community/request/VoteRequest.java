package com.example.lolserver.controller.community.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank String voteType
) {
}
