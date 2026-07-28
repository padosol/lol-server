package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank String content
) {
}
