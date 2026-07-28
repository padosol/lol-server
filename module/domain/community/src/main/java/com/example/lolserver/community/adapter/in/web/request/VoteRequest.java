package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank String voteType
) {
}
