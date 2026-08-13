package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code categoryId} 는 카테고리 트리 응답({@code GET /api/community/categories})의 id 를
 * 그대로 되돌려 보내는 값이다. 존재·작성 가능 여부는 서비스가 검증한다.
 */
public record CreatePostRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotNull Long categoryId
) {
}
