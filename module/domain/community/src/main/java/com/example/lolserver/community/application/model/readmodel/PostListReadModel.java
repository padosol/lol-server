package com.example.lolserver.community.application.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class PostListReadModel {

    private final Long id;
    private final String title;
    private final Long categoryId;
    private final int viewCount;
    private final int upvoteCount;
    private final int downvoteCount;
    private final int commentCount;
    private final double hotScore;
    /**
     * 본문에 붙은 이미지가 있는지. 목록이 사진 아이콘을 붙일지 판단하는 데만 쓴다.
     *
     * <p>몇 장인지는 내려주지 않는다 — 목록은 "사진 있음"만 보여주므로 개수를 세면
     * 쓰지도 않을 값을 위해 GROUP BY 를 하게 된다.
     */
    private final boolean hasImage;
    /**
     * 작성자(member) 식별자. 영속성 어댑터가 채우며,
     * 애플리케이션 서비스가 member port.in 으로 author 를 배치 보강할 때 조인 키로 쓴다.
     */
    private final Long authorId;
    private final AuthorReadModel author;
    private final LocalDateTime createdAt;
}
