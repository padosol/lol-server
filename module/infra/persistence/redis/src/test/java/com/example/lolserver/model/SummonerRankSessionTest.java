package com.example.lolserver.model;

import com.example.lolserver.Division;
import com.example.lolserver.QueueType;
import com.example.lolserver.Tier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummonerRankSessionTest {

    @DisplayName("키가 있는 경우 hasKey가 true를 반환한다")
    @Test
    void hasKey_withKey_returnsTrue() {
        // given
        SummonerRankSession session = new SummonerRankSession();
        session.setKey("some-key-value");

        // when
        boolean result = session.hasKey();

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("키가 null인 경우 hasKey가 false를 반환한다")
    @Test
    void hasKey_nullKey_returnsFalse() {
        // given
        SummonerRankSession session = new SummonerRankSession();
        session.setKey(null);

        // when
        boolean result = session.hasKey();

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("키가 빈 문자열인 경우 hasKey가 false를 반환한다")
    @Test
    void hasKey_emptyKey_returnsFalse() {
        // given
        SummonerRankSession session = new SummonerRankSession();
        session.setKey("");

        // when
        boolean result = session.hasKey();

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("키가 공백만 있는 경우 hasKey가 false를 반환한다")
    @Test
    void hasKey_whitespaceKey_returnsFalse() {
        // given
        SummonerRankSession session = new SummonerRankSession();
        session.setKey("   ");

        // when
        boolean result = session.hasKey();

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("모든 필드로 SummonerRankSession을 생성할 수 있다")
    @Test
    void constructor_allArgs_createsSession() {
        // given & when
        SummonerRankSession session = new SummonerRankSession(
                QueueType.RANKED_SOLO_5x5,
                "TestPlayer",
                "KR1",
                "summoner-id",
                "league-id",
                100,
                50,
                75,
                Tier.DIAMOND,
                Division.I,
                "test-puuid",
                300L,
                "TOP",
                List.of("Garen", "Darius"),
                2500.0,
                "rank-key"
        );

        // then
        assertThat(session.getQueueType()).isEqualTo(QueueType.RANKED_SOLO_5x5);
        assertThat(session.getSummonerName()).isEqualTo("TestPlayer");
        assertThat(session.getTagLine()).isEqualTo("KR1");
        assertThat(session.getSummonerId()).isEqualTo("summoner-id");
        assertThat(session.getLeagueId()).isEqualTo("league-id");
        assertThat(session.getWin()).isEqualTo(100);
        assertThat(session.getLosses()).isEqualTo(50);
        assertThat(session.getPoint()).isEqualTo(75);
        assertThat(session.getTier()).isEqualTo(Tier.DIAMOND);
        assertThat(session.getDivision()).isEqualTo(Division.I);
        assertThat(session.getPuuid()).isEqualTo("test-puuid");
        assertThat(session.getSummonerLevel()).isEqualTo(300L);
        assertThat(session.getPosition()).isEqualTo("TOP");
        assertThat(session.getChampionNames()).containsExactly("Garen", "Darius");
        assertThat(session.getScore()).isEqualTo(2500.0);
        assertThat(session.getKey()).isEqualTo("rank-key");
        assertThat(session.hasKey()).isTrue();
    }

    @DisplayName("NoArgsConstructor로 생성하고 Setter로 값을 설정할 수 있다")
    @Test
    void noArgsConstructor_setters_workCorrectly() {
        // given
        SummonerRankSession session = new SummonerRankSession();

        // when
        session.setQueueType(QueueType.RANKED_FLEX_SR);
        session.setSummonerName("FlexPlayer");
        session.setTagLine("KR2");
        session.setTier(Tier.PLATINUM);
        session.setDivision(Division.II);
        session.setWin(80);
        session.setLosses(60);

        // then
        assertThat(session.getQueueType()).isEqualTo(QueueType.RANKED_FLEX_SR);
        assertThat(session.getSummonerName()).isEqualTo("FlexPlayer");
        assertThat(session.getTagLine()).isEqualTo("KR2");
        assertThat(session.getTier()).isEqualTo(Tier.PLATINUM);
        assertThat(session.getDivision()).isEqualTo(Division.II);
        assertThat(session.getWin()).isEqualTo(80);
        assertThat(session.getLosses()).isEqualTo(60);
    }
}
