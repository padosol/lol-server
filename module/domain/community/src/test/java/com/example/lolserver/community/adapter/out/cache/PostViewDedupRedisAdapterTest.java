package com.example.lolserver.community.adapter.out.cache;

import com.example.lolserver.community.domain.vo.ViewerKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostViewDedupRedisAdapterTest {

    private static final String KEY = "community:post-view:42:m:7";
    private static final Duration TTL = Duration.ofHours(24);

    @Mock
    private StringRedisTemplate template;

    @Mock
    private ValueOperations<String, String> ops;

    @InjectMocks
    private PostViewDedupRedisAdapter adapter;

    @BeforeEach
    void setUp() {
        given(template.opsForValue()).willReturn(ops);
    }

    @Test
    void 처음이면_true() {
        given(ops.setIfAbsent(KEY, "1", TTL)).willReturn(Boolean.TRUE);

        assertThat(adapter.tryMarkViewed(42L, ViewerKey.ofMember(7L), TTL)).isTrue();
    }

    @Test
    void 이미_있으면_false() {
        given(ops.setIfAbsent(KEY, "1", TTL)).willReturn(Boolean.FALSE);

        assertThat(adapter.tryMarkViewed(42L, ViewerKey.ofMember(7L), TTL)).isFalse();
    }

    @Test
    void 결과가_null_이면_false() {
        given(ops.setIfAbsent(KEY, "1", TTL)).willReturn(null);

        assertThat(adapter.tryMarkViewed(42L, ViewerKey.ofMember(7L), TTL)).isFalse();
    }

    @Test
    void Redis_예외면_false_를_반환하고_전파하지_않는다() {
        given(ops.setIfAbsent(KEY, "1", TTL)).willThrow(new RedisConnectionFailureException("down"));

        assertThat(adapter.tryMarkViewed(42L, ViewerKey.ofMember(7L), TTL)).isFalse();
    }
}
