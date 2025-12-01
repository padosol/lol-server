package com.example.lolserver.storage.db.core.repository.summoner.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

class SummonerTest {

    @DisplayName("갱신 한지 3분 이후, 갱신 클릭한지 10초 이후 갱신이 가능하다.")
    @Test
    void revision_test() {

        // given
        LocalDateTime revisionDateTime = LocalDateTime.of(
                2025, 11, 30, 10, 30, 10);

        LocalDateTime revisionClickDateTime = LocalDateTime.of(
                2025, 11, 30, 10, 30, 20);

        Summoner summoner = new Summoner(
            "summoner_puuid",
                1,
                revisionDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                100,
                "hide on bush",
                "kr1",
                "KR",
                "hideonbush#kr1",
                revisionClickDateTime
        );

        // when
        LocalDateTime isPossibleClickDateTime = LocalDateTime.of(
                2025, 11, 30, 10, 34, 0);
        boolean revision1 = summoner.isRevision(isPossibleClickDateTime);

        // 3분이 넘지 않았음.
        LocalDateTime isImpossibleClickDateTime = LocalDateTime.of(
                2025, 11, 30, 10, 33, 0);
        boolean revision2 = summoner.isRevision(isImpossibleClickDateTime);

        // 10초가 되지 않았음.
        LocalDateTime isImpossibleClickDateTime2 = LocalDateTime.of(
                2025, 11, 30, 10, 30, 25);
        boolean revision3 = summoner.isRevision(isImpossibleClickDateTime2);

        // then
        Assertions.assertThat(revision1).isTrue();
        Assertions.assertThat(revision2).isFalse();
        Assertions.assertThat(revision3).isFalse();
    }

}