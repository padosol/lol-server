package com.example.lolserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SummonerCacheAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SummonerCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SummonerCacheAdapter(stringRedisTemplate);
    }

    @DisplayName("puuid가 캐시에 존재하면 isUpdating은 true를 반환한다")
    @Test
    void isUpdating_existingPuuid_returnsTrue() {
        // given
        String puuid = "test-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(puuid)).willReturn(puuid);

        // when
        boolean result = adapter.isUpdating(puuid);

        // then
        assertThat(result).isTrue();
        then(stringRedisTemplate).should().opsForValue();
        then(valueOperations).should().get(puuid);
    }

    @DisplayName("puuid가 캐시에 존재하지 않으면 isUpdating은 false를 반환한다")
    @Test
    void isUpdating_nonExistingPuuid_returnsFalse() {
        // given
        String puuid = "test-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(puuid)).willReturn(null);

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
        given(valueOperations.get(puuid)).willReturn("");

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
        then(valueOperations).should().set(puuid, puuid);
    }

    @DisplayName("puuid가 캐시에 존재하면 isSummonerRenewal은 true를 반환한다")
    @Test
    void isSummonerRenewal_existingPuuid_returnsTrue() {
        // given
        String puuid = "renewal-puuid-123";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(puuid)).willReturn(puuid);

        // when
        boolean result = adapter.isSummonerRenewal(puuid);

        // then
        assertThat(result).isTrue();
        then(valueOperations).should().get(puuid);
    }

    @DisplayName("puuid가 캐시에 존재하지 않으면 isSummonerRenewal은 false를 반환한다")
    @Test
    void isSummonerRenewal_nonExistingPuuid_returnsFalse() {
        // given
        String puuid = "non-existing-puuid";
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(puuid)).willReturn(null);

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
        given(valueOperations.get(puuid)).willReturn("   ");

        // when
        boolean result = adapter.isSummonerRenewal(puuid);

        // then
        assertThat(result).isFalse();
    }
}
