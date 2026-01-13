package com.example.lolserver.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerRenewalSessionTest {

    @DisplayName("PUUID만으로 SummonerRenewalSession을 생성하면 모든 업데이트 플래그가 false이다")
    @Test
    void constructor_puuidOnly_allFlagsFalse() {
        // given & when
        SummonerRenewalSession session = new SummonerRenewalSession("test-puuid");

        // then
        assertThat(session.getPuuid()).isEqualTo("test-puuid");
        assertThat(session.isSummonerUpdate()).isFalse();
        assertThat(session.isLeagueUpdate()).isFalse();
        assertThat(session.isMatchUpdate()).isFalse();
    }

    @DisplayName("AllArgsConstructor로 모든 필드 값을 설정할 수 있다")
    @Test
    void constructor_allArgs_setsAllFields() {
        // given & when
        SummonerRenewalSession session = new SummonerRenewalSession(
                "test-puuid",
                true,
                true,
                true
        );

        // then
        assertThat(session.getPuuid()).isEqualTo("test-puuid");
        assertThat(session.isSummonerUpdate()).isTrue();
        assertThat(session.isLeagueUpdate()).isTrue();
        assertThat(session.isMatchUpdate()).isTrue();
    }

    @DisplayName("NoArgsConstructor로 생성 후 Setter로 값을 설정할 수 있다")
    @Test
    void noArgsConstructor_setters_workCorrectly() {
        // given
        SummonerRenewalSession session = new SummonerRenewalSession();

        // when
        session.setPuuid("new-puuid");
        session.setSummonerUpdate(true);
        session.setLeagueUpdate(true);
        session.setMatchUpdate(true);

        // then
        assertThat(session.getPuuid()).isEqualTo("new-puuid");
        assertThat(session.isSummonerUpdate()).isTrue();
        assertThat(session.isLeagueUpdate()).isTrue();
        assertThat(session.isMatchUpdate()).isTrue();
    }

    @DisplayName("업데이트 플래그를 개별적으로 토글할 수 있다")
    @Test
    void setters_individualFlags_workIndependently() {
        // given
        SummonerRenewalSession session = new SummonerRenewalSession("test-puuid");

        // when
        session.setSummonerUpdate(true);
        // leagueUpdate and matchUpdate remain false

        // then
        assertThat(session.isSummonerUpdate()).isTrue();
        assertThat(session.isLeagueUpdate()).isFalse();
        assertThat(session.isMatchUpdate()).isFalse();
    }
}
