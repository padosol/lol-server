package com.example.lolserver.community.adapter.in.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * {@code imageIds} 는 <b>전체 교체 시맨틱</b>이다 — 요청에 없는 기존 첨부는 DETACHED 로
 * 전이된다. 부분 갱신이 아니므로 수정 화면은 유지할 이미지를 모두 담아 보내야 한다
 * (그래서 상세 응답이 현재 첨부 목록을 함께 내려준다).
 */
public record UpdatePostRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotNull Long categoryId,
        @Size(max = 10) List<Long> imageIds
) {
}
