package com.example.lolserver.member.adapter.in.web.security.oauth2;

import com.example.lolserver.member.application.model.OAuthUserInfo;
import com.example.lolserver.member.domain.vo.RiotAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RiotOAuth2UserInfoExtractorTest {

    private final RiotOAuth2UserInfoExtractor extractor = new RiotOAuth2UserInfoExtractor();

    @DisplayName("CustomOidcUserService 가 붙인 puuid·Riot ID 를 연동 정보로 옮긴다")
    @Test
    void extract_withRiotId() {
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(), Map.of(
                "sub", "riot-sub", "puuid", "riot-puuid",
                "gameName", "Hide on bush", "tagLine", "KR1"), "sub");

        OAuthUserInfo info = extractor.extract(user);

        assertThat(info.getProvider()).isEqualTo("RIOT");
        assertThat(info.getProviderId()).isEqualTo("riot-sub");
        assertThat(info.toRiotAccount()).isEqualTo(new RiotAccount("riot-puuid", "Hide on bush", "KR1"));
    }

    @DisplayName("Riot ID 가 없으면 puuid 만 연동된다")
    @Test
    void extract_withoutRiotId() {
        DefaultOAuth2User user = new DefaultOAuth2User(List.of(), Map.of(
                "sub", "riot-sub", "puuid", "riot-puuid"), "sub");

        OAuthUserInfo info = extractor.extract(user);

        assertThat(info.toRiotAccount()).isEqualTo(new RiotAccount("riot-puuid", null, null));
    }
}
