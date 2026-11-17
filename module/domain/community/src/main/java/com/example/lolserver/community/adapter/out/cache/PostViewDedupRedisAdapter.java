package com.example.lolserver.community.adapter.out.cache;

import com.example.lolserver.community.application.port.out.PostViewDedupPort;
import com.example.lolserver.community.domain.vo.ViewerKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis {@code SET NX + TTL} 로 (게시글, 조회자) 쌍의 중복 조회를 판정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewDedupRedisAdapter implements PostViewDedupPort {

    private static final String KEY_PREFIX = "community:post-view:";
    private static final String MARKER = "1";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryMarkViewed(Long postId, ViewerKey viewer, Duration ttl) {
        String key = KEY_PREFIX + postId + ":" + viewer.value();
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, MARKER, ttl));
        } catch (RuntimeException e) {
            // RedisImageRateLimitAdapter 와 달리 fail-closed: 장애 시 세지 않는다(false).
            // 장애 중에 통과시키면 새로고침 조회수 부풀리기가 되살아나므로 과소 집계를 택한다.
            log.warn("게시글 조회 중복 판정 실패, 조회수를 올리지 않는다: postId={}, viewer={}",
                    postId, viewer.value(), e);
            return false;
        }
    }
}
