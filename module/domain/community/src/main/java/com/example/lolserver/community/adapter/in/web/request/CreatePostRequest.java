package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * {@code categoryId} 는 카테고리 트리 응답({@code GET /api/community/categories})의 id 를
 * 그대로 되돌려 보내는 값이다. 존재·작성 가능 여부는 서비스가 검증한다.
 *
 * <p>{@code imageIds} 는 {@code POST /api/community/images} 로 먼저 올린 이미지의 id 다.
 * 본문에서 URL 을 정규식으로 긁는 대신 클라이언트가 자기가 올린 id 를 돌려주게 한 이유는,
 * 마크다운/HTML/에디터 방언마다 파싱이 깨지고 외부 URL 과 우리 URL 을 구분하는 로직이
 * 또 필요해지기 때문이다. id 를 받으면 소유자·상태만 보면 된다.
 *
 * <p>선택 필드다 — 없으면 첨부 없음으로 처리되어 기존 클라이언트가 깨지지 않는다.
 */
public record CreatePostRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotNull Long categoryId,
        // 실제 상한의 진실원천은 community.image.max-count-per-post 다. 어노테이션은 상수만
        // 받을 수 있어 값을 중복해 두되, 초과 판정은 서비스가 설정값으로 다시 한다.
        @Size(max = 10) List<Long> imageIds
) {
}
