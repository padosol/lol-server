package com.example.lolserver.controller.community.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotBlank String category
) {
}
