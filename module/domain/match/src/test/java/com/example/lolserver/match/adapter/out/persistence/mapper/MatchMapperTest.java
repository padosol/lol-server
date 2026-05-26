package com.example.lolserver.match.adapter.out.persistence.mapper;

import com.example.lolserver.match.application.model.GameInfoReadModel;
import com.example.lolserver.match.application.model.ItemValueReadModel;
import com.example.lolserver.match.application.model.MSChampionDetailReadModel;
import com.example.lolserver.match.application.model.ParticipantReadModel;
import com.example.lolserver.match.application.model.StatValueReadModel;
import com.example.lolserver.match.application.model.StyleReadModel;
import com.example.lolserver.match.adapter.out.persistence.dto.MSChampionDTO;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchEntity;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchSummonerEntity;
import com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.PerkStatValue;
import com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.PerkStyleValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchMapperTest {

    private final MatchMapper matchMapper = MatchMapper.INSTANCE;

    @DisplayName("MatchEntity를 GameInfoReadModel로 변환한다")
    @Test
    void toGameInfoReadModel_validEntity_returnsGameInfoReadModel() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12345")
                .queueId(420)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .gameType("MATCHED_GAME")
                .gameVersion("14.1.1")
                .averageTier(4175)
                .build();

        // when
        GameInfoReadModel result = matchMapper.toGameInfoReadModel(matchEntity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo("KR_12345");
        assertThat(result.getQueueId()).isEqualTo(420);
        assertThat(result.getGameDuration()).isEqualTo(1800L);
        assertThat(result.getGameMode()).isEqualTo("CLASSIC");
        assertThat(result.getAverageTier()).isEqualTo("GOLD");
        assertThat(result.getAverageRank()).isEqualTo("I");
    }

    @DisplayName("MatchEntity의 averageTier가 IRON 영역이면 IRON + Division으로 변환한다")
    @Test
    void toGameInfoReadModel_ironTier_convertsCorrectly() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12346")
                .averageTier(1600)
                .build();

        // when
        GameInfoReadModel result = matchMapper.toGameInfoReadModel(matchEntity);

        // then
        assertThat(result.getAverageTier()).isEqualTo("IRON");
        assertThat(result.getAverageRank()).isEqualTo("IV");
    }

    @DisplayName("MatchEntity의 averageTier가 MASTER 이상이면 averageRank는 null이다")
    @Test
    void toGameInfoReadModel_masterTier_rankIsNull() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12347")
                .averageTier(8200)
                .build();

        // when
        GameInfoReadModel result = matchMapper.toGameInfoReadModel(matchEntity);

        // then
        assertThat(result.getAverageTier()).isEqualTo("MASTER");
        assertThat(result.getAverageRank()).isNull();
    }

    @DisplayName("MatchEntity의 averageTier가 null이면 둘 다 null이다")
    @Test
    void toGameInfoReadModel_nullAverageTier_bothNull() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12348")
                .averageTier(null)
                .build();

        // when
        GameInfoReadModel result = matchMapper.toGameInfoReadModel(matchEntity);

        // then
        assertThat(result.getAverageTier()).isNull();
        assertThat(result.getAverageRank()).isNull();
    }

    @DisplayName("MatchSummonerEntity를 ParticipantReadModel로 변환한다")
    @Test
    void toReadModel_matchSummonerEntity_returnsParticipantReadModel() {
        // given
        MatchSummonerEntity entity = MatchSummonerEntity.builder()
                .puuid("test-puuid")
                .matchId("KR_12345")
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .totalDamageDealtToChampions(25000)
                .goldEarned(15000)
                .build();

        // when
        ParticipantReadModel result = matchMapper.toReadModel(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1);
        assertThat(result.getChampionId()).isEqualTo(157);
        assertThat(result.getChampionName()).isEqualTo("Yasuo");
        assertThat(result.getKills()).isEqualTo(10);
        assertThat(result.getDeaths()).isEqualTo(5);
        assertThat(result.getAssists()).isEqualTo(8);
        assertThat(result.isWin()).isTrue();
    }

    @DisplayName("MSChampionDTO를 MSChampionDetailReadModel로 변환한다")
    @Test
    void toReadModel_msChampionDTO_returnsMSChampionDetailReadModel() {
        // given
        MSChampionDTO dto = new MSChampionDTO(
                8.0, 5.0, 10.0,
                157, "Yasuo",
                15L, 5L,
                500.0, 3.6, 65.0, 25.0, 400.0, 20L,
                420
        );

        // when
        MSChampionDetailReadModel result = matchMapper.toReadModel(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChampionId()).isEqualTo(157);
        assertThat(result.getChampionName()).isEqualTo("Yasuo");
        assertThat(result.getPlayCount()).isEqualTo(20L);
    }

    @DisplayName("PerkStyleValue를 StyleReadModel로 변환한다")
    @Test
    void toReadModel_perkStyleValue_returnsStyleReadModel() {
        // given
        PerkStyleValue perkStyleValue = PerkStyleValue.builder()
                .primaryStyleId(8000)
                .primaryPerk0(8005)
                .primaryPerk1(9111)
                .primaryPerk2(9104)
                .primaryPerk3(8299)
                .subStyleId(8100)
                .subPerk0(8139)
                .subPerk1(8135)
                .build();

        // when
        StyleReadModel result = matchMapper.toReadModel(perkStyleValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryStyleId()).isEqualTo(8000);
        assertThat(result.getPrimaryPerk0()).isEqualTo(8005);
        assertThat(result.getSubStyleId()).isEqualTo(8100);
        assertThat(result.getSubPerk0()).isEqualTo(8139);
    }

    @DisplayName("ItemValue 엔티티를 ItemValueReadModel로 변환한다")
    @Test
    void toReadModel_itemValue_returnsItemValueReadModel() {
        // given
        com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.ItemValue entityItemValue =
                com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.ItemValue.builder()
                        .item0(3006)
                        .item1(3009)
                        .item2(3047)
                        .item3(3071)
                        .item4(3153)
                        .item5(3508)
                        .item6(3340)
                        .build();

        // when
        ItemValueReadModel result = matchMapper.toReadModel(entityItemValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItem0()).isEqualTo(3006);
        assertThat(result.getItem1()).isEqualTo(3009);
        assertThat(result.getItem6()).isEqualTo(3340);
    }

    @DisplayName("PerkStatValue 엔티티를 StatValueReadModel로 변환한다")
    @Test
    void toReadModel_perkStatValue_returnsStatValueReadModel() {
        // given
        PerkStatValue perkStatValue = PerkStatValue.builder()
                .statPerkDefense(5002)
                .statPerkFlex(5008)
                .statPerkOffense(5005)
                .build();

        // when
        StatValueReadModel result = matchMapper.toReadModel(perkStatValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDefense()).isEqualTo(5002);
        assertThat(result.getFlex()).isEqualTo(5008);
        assertThat(result.getOffense()).isEqualTo(5005);
    }
}
