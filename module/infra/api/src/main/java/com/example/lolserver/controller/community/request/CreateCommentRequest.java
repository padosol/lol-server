package com.example.lolserver.controller.community.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank String content,
        Long parentCommentId
) {
}
