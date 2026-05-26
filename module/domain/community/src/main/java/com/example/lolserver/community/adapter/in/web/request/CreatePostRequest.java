package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotBlank String category
) {
}
