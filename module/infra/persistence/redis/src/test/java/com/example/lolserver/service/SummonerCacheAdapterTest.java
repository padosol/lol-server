package com.example.lolserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SummonerCacheAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SummonerCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SummonerCacheAdapter(stringRedisTemplate, redissonClient);
    }

    @DisplayName("puuid가 캐시에 존재하면 isUpdating은 true를 반환한다")
    @Test
    void isUpdating_existingPuuid_returnsTrue() {
        // given
        String puuid = "test-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("summoner:renewal:" + puuid)).willReturn(puuid);

        // when
        boolean result = adapter.isUpdating(puuid);

        // then
        assertThat(result).isTrue();
        then(stringRedisTemplate).should().opsForValue();
        then(valueOperations).should().get("summoner:renewal:" + puuid);
    }

    @DisplayName("puuid가 캐시에 존재하지 않으면 isUpdating은 false를 반환한다")
    @Test
    void isUpdating_nonExistingPuuid_returnsFalse() {
        // given
        String puuid = "test-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("summoner:renewal:" + puuid)).willReturn(null);

        // when
        boolean result = adapter.isUpdating(puuid);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("puuid가 빈 문자열이면 isUpdating은 false를 반환한다")
    @Test
    void isUpdating_emptyString_returnsFalse() {
        // given
        String puuid = "test-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("summoner:renewal:" + puuid)).willReturn("");

        // when
        boolean result = adapter.isUpdating(puuid);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("createSummonerRenewal이 puuid를 캐시에 저장한다")
    @Test
    void createSummonerRenewal_validPuuid_savesToCache() {
        // given
        String puuid = "test-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        adapter.createSummonerRenewal(puuid);

        // then
        then(stringRedisTemplate).should().opsForValue();
        then(valueOperations).should().set("summoner:renewal:" + puuid, puuid);
    }

    @DisplayName("puuid가 캐시에 존재하면 isSummonerRenewal은 true를 반환한다")
    @Test
    void isSummonerRenewal_existingPuuid_returnsTrue() {
        // given
        String puuid = "renewal-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("summoner:renewal:" + puuid)).willReturn(puuid);

        // when
        boolean result = adapter.isSummonerRenewal(puuid);

        // then
        assertThat(result).isTrue();
        then(valueOperations).should().get("summoner:renewal:" + puuid);
    }

    @DisplayName("puuid가 캐시에 존재하지 않으면 isSummonerRenewal은 false를 반환한다")
    @Test
    void isSummonerRenewal_nonExistingPuuid_returnsFalse() {
        // given
        String puuid = "non-existing-puuid";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("summoner:renewal:" + puuid)).willReturn(null);

        // when
        boolean result = adapter.isSummonerRenewal(puuid);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("공백만 있는 값은 isSummonerRenewal이 false를 반환한다")
    @Test
    void isSummonerRenewal_whitespaceOnly_returnsFalse() {
        // given
        String puuid = "test-puuid";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("summoner:renewal:" + puuid)).willReturn("   ");

        // when
        boolean result = adapter.isSummonerRenewal(puuid);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("tryLock이 락 획득에 성공하면 true를 반환한다")
    @Test
    void tryLock_successfulLock_returnsTrue() throws InterruptedException {
        // given
        String key = "test-key";
        given(redissonClient.getLock("summoner:lock:" + key)).willReturn(rLock);
        given(rLock.tryLock(5L, 10L, TimeUnit.SECONDS)).willReturn(true);

        // when
        boolean result = adapter.tryLock(key);

        // then
        assertThat(result).isTrue();
        then(redissonClient).should().getLock("summoner:lock:" + key);
        then(rLock).should().tryLock(5L, 10L, TimeUnit.SECONDS);
    }

    @DisplayName("tryLock이 락 획득에 실패하면 false를 반환한다")
    @Test
    void tryLock_failedLock_returnsFalse() throws InterruptedException {
        // given
        String key = "test-key";
        given(redissonClient.getLock("summoner:lock:" + key)).willReturn(rLock);
        given(rLock.tryLock(5L, 10L, TimeUnit.SECONDS)).willReturn(false);

        // when
        boolean result = adapter.tryLock(key);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("tryLock에서 InterruptedException 발생 시 false를 반환한다")
    @Test
    void tryLock_interrupted_returnsFalse() throws InterruptedException {
        // given
        String key = "test-key";
        given(redissonClient.getLock("summoner:lock:" + key)).willReturn(rLock);
        given(rLock.tryLock(5L, 10L, TimeUnit.SECONDS)).willThrow(new InterruptedException());

        // when
        boolean result = adapter.tryLock(key);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("unlock이 현재 스레드가 락을 보유하고 있으면 락을 해제한다")
    @Test
    void unlock_heldByCurrentThread_releasesLock() {
        // given
        String key = "test-key";
        given(redissonClient.getLock("summoner:lock:" + key)).willReturn(rLock);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // when
        adapter.unlock(key);

        // then
        then(rLock).should().unlock();
    }

    @DisplayName("unlock이 현재 스레드가 락을 보유하지 않으면 아무 동작도 하지 않는다")
    @Test
    void unlock_notHeldByCurrentThread_doesNothing() {
        // given
        String key = "test-key";
        given(redissonClient.getLock("summoner:lock:" + key)).willReturn(rLock);
        given(rLock.isHeldByCurrentThread()).willReturn(false);

        // when
        adapter.unlock(key);

        // then
        then(rLock).should().isHeldByCurrentThread();
        then(rLock).shouldHaveNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    @DisplayName("getRefreshingPuuids가 갱신 중인 puuid 목록을 반환한다")
    @Test
    void getRefreshingPuuids_existingKeys_returnsPuuids() {
        // given
        Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
        given(stringRedisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
        given(cursor.hasNext()).willReturn(true, true, false);
        given(cursor.next()).willReturn("summoner:renewal:puuid-1", "summoner:renewal:puuid-2");

        // when
        Set<String> result = adapter.getRefreshingPuuids();

        // then
        assertThat(result).containsExactlyInAnyOrder("puuid-1", "puuid-2");
    }

    @SuppressWarnings("unchecked")
    @DisplayName("getRefreshingPuuids가 갱신 중인 키가 없으면 빈 Set을 반환한다")
    @Test
    void getRefreshingPuuids_noKeys_returnsEmptySet() {
        // given
        Cursor<String> cursor = org.mockito.Mockito.mock(Cursor.class);
        given(stringRedisTemplate.scan(any(ScanOptions.class))).willReturn(cursor);
        given(cursor.hasNext()).willReturn(false);

        // when
        Set<String> result = adapter.getRefreshingPuuids();

        // then
        assertThat(result).isEmpty();
    }
}
