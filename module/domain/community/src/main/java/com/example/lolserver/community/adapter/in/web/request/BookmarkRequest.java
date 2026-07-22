package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkRequest(
        @NotNull Long postId
) {
}
