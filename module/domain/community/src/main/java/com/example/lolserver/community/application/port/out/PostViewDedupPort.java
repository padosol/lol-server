package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.domain.vo.ViewerKey;

import java.time.Duration;

public interface PostViewDedupPort {

    /** 창 안의 첫 조회일 때만 true. 중복이거나 저장소 장애면 false. */
    boolean tryMarkViewed(Long postId, ViewerKey viewer, Duration ttl);
}
