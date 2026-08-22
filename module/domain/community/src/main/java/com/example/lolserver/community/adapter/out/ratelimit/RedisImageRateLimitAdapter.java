package com.example.lolserver.community.adapter.out.ratelimit;

import com.example.lolserver.community.application.port.out.ImageRateLimitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 회원당 분당 업로드 횟수 카운터(고정 윈도우).
 *
 * <p>키에 분 단위 버킷을 넣어 윈도우가 저절로 롤오버되게 한다 — 별도 만료 관리가 필요 없고,
 * TTL 은 버킷이 지난 뒤 키를 치우는 용도로만 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisImageRateLimitAdapter implements ImageRateLimitPort {

    private static final String KEY_PREFIX = "community:image:upload:";
    private static final DateTimeFormatter MINUTE_BUCKET =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Duration TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryAcquire(Long memberId, int limitPerMinute) {
        // 존은 무엇이든 창이 1분으로 유지되지만, 명시하지 않으면 인스턴스마다 다른 존을
        // 잡아 같은 회원이 서로 다른 버킷 키를 써 한도가 사실상 배로 늘어난다.
        String key = KEY_PREFIX + memberId + ":"
                + LocalDateTime.now(ZoneId.systemDefault()).format(MINUTE_BUCKET);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                stringRedisTemplate.expire(key, TTL);
            }
            return count <= limitPerMinute;
        } catch (RuntimeException e) {
            // Redis 장애로 업로드 전체가 막히는 편이 남용 위험보다 크다고 판단해 fail-open 한다.
            // 미인증 업로드는 SecurityConfig 가 이미 차단하고 있어 무제한 노출은 아니다.
            log.warn("업로드 rate limit 카운터 조회 실패, 통과시킨다: memberId={}", memberId, e);
            return true;
        }
    }
}
