package com.example.lolserver.controller.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SocialAccountLinkTokenStore 테스트")
class SocialAccountLinkTokenStoreTest {

    private final SocialAccountLinkTokenStore store =
            new SocialAccountLinkTokenStore();

    @DisplayName("토큰 생성 및 소비 - 정상 동작")
    @Test
    void generateAndConsumeToken() {
        // given
        Long memberId = 42L;

        // when
        String token = store.generateToken(memberId);
        Long result = store.consumeToken(token);

        // then
        assertThat(token).isNotNull();
        assertThat(result).isEqualTo(memberId);
    }

    @DisplayName("토큰 이중 소비 - 두 번째 소비 시 null 반환")
    @Test
    void consumeTokenTwice() {
        // given
        String token = store.generateToken(1L);

        // when
        store.consumeToken(token);
        Long secondResult = store.consumeToken(token);

        // then
        assertThat(secondResult).isNull();
    }

    @DisplayName("존재하지 않는 토큰 소비 - null 반환")
    @Test
    void consumeUnknownToken() {
        // when
        Long result = store.consumeToken("unknown-token");

        // then
        assertThat(result).isNull();
    }

    @DisplayName("서로 다른 회원의 토큰이 독립적으로 동작")
    @Test
    void independentTokensForDifferentMembers() {
        // given
        String token1 = store.generateToken(1L);
        String token2 = store.generateToken(2L);

        // when & then
        assertThat(store.consumeToken(token1)).isEqualTo(1L);
        assertThat(store.consumeToken(token2)).isEqualTo(2L);
    }
}
