package com.example.lolserver.repository.match.mapper;

import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.match.domain.gamedata.TeamInfoData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import com.example.lolserver.domain.match.domain.gamedata.value.ItemValue;
import com.example.lolserver.domain.match.domain.gamedata.value.StatValue;
import com.example.lolserver.domain.match.domain.gamedata.value.Style;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStyleValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchMapperTest {

    private final MatchMapper matchMapper = MatchMapper.INSTANCE;

    @DisplayName("MatchEntity를 GameInfoData로 변환한다")
    @Test
    void toGameInfoData_validEntity_returnsGameInfoData() {
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
        GameInfoData result = matchMapper.toGameInfoData(matchEntity);

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
    void toGameInfoData_ironTier_convertsCorrectly() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12346")
                .averageTier(1600)
                .build();

        // when
        GameInfoData result = matchMapper.toGameInfoData(matchEntity);

        // then
        assertThat(result.getAverageTier()).isEqualTo("IRON");
        assertThat(result.getAverageRank()).isEqualTo("IV");
    }

    @DisplayName("MatchEntity의 averageTier가 MASTER 이상이면 averageRank는 null이다")
    @Test
    void toGameInfoData_masterTier_rankIsNull() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12347")
                .averageTier(8200)
                .build();

        // when
        GameInfoData result = matchMapper.toGameInfoData(matchEntity);

        // then
        assertThat(result.getAverageTier()).isEqualTo("MASTER");
        assertThat(result.getAverageRank()).isNull();
    }

    @DisplayName("MatchEntity의 averageTier가 null이면 둘 다 null이다")
    @Test
    void toGameInfoData_nullAverageTier_bothNull() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12348")
                .averageTier(null)
                .build();

        // when
        GameInfoData result = matchMapper.toGameInfoData(matchEntity);

        // then
        assertThat(result.getAverageTier()).isNull();
        assertThat(result.getAverageRank()).isNull();
    }

    @DisplayName("MatchSummonerEntity를 ParticipantData로 변환한다")
    @Test
    void toDomain_matchSummonerEntity_returnsParticipantData() {
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
        ParticipantData result = matchMapper.toDomain(entity);

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

    @DisplayName("MatchTeamEntity를 TeamInfoData로 변환한다")
    @Test
    void toDomain_matchTeamEntity_returnsTeamInfoData() {
        // given
        MatchTeamEntity entity = MatchTeamEntity.builder()
                .matchId("KR_12345")
                .teamId(100)
                .win(true)
                .build();

        // when
        TeamInfoData result = matchMapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTeamId()).isEqualTo(100);
        assertThat(result.isWin()).isTrue();
    }

    @DisplayName("MSChampionDTO를 MSChampion 도메인으로 변환한다")
    @Test
    void toDomain_msChampionDTO_returnsMSChampion() {
        // given
        MSChampionDTO dto = new MSChampionDTO(
                8.0, 5.0, 10.0,
                157, "Yasuo",
                15L, 5L,
                500.0, 3.6, 65.0, 25.0, 400.0, 20L
        );

        // when
        MSChampion result = matchMapper.toDomain(dto);

        // then
        assertThat(result).isNotNull();
    }

    @DisplayName("ItemEventsEntity를 ItemEvents 도메인으로 변환한다")
    @Test
    void toDomain_itemEventsEntity_returnsItemEvents() {
        // given
        ItemEventsEntity entity = new ItemEventsEntity();
        entity.setParticipantId(1);
        entity.setItemId(3006);
        entity.setTimestamp(60000L);
        entity.setType("ITEM_PURCHASED");

        // when
        ItemEvents result = matchMapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1);
        assertThat(result.getItemId()).isEqualTo(3006);
        assertThat(result.getTimestamp()).isEqualTo(60000L);
        assertThat(result.getType()).isEqualTo("ITEM_PURCHASED");
    }

    @DisplayName("SkillEventsEntity를 SkillEvents 도메인으로 변환한다")
    @Test
    void toDomain_skillEventsEntity_returnsSkillEvents() {
        // given
        SkillEventsEntity entity = new SkillEventsEntity();
        entity.setParticipantId(1);
        entity.setSkillSlot(1);
        entity.setTimestamp(30000L);

        // when
        SkillEvents result = matchMapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1);
        assertThat(result.getSkillSlot()).isEqualTo(1);
        assertThat(result.getTimestamp()).isEqualTo(30000L);
    }

    @DisplayName("ItemEventsEntity 리스트를 ItemEvents 도메인 리스트로 변환한다")
    @Test
    void toDomainItemEventsList_entityList_returnsDomainList() {
        // given
        ItemEventsEntity entity1 = new ItemEventsEntity();
        entity1.setParticipantId(1);
        entity1.setItemId(3006);
        entity1.setTimestamp(60000L);
        entity1.setType("ITEM_PURCHASED");

        ItemEventsEntity entity2 = new ItemEventsEntity();
        entity2.setParticipantId(2);
        entity2.setItemId(3009);
        entity2.setTimestamp(90000L);
        entity2.setType("ITEM_PURCHASED");

        List<ItemEventsEntity> entities = List.of(entity1, entity2);

        // when
        List<ItemEvents> result = matchMapper.toDomainItemEventsList(entities);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemId()).isEqualTo(3006);
        assertThat(result.get(1).getItemId()).isEqualTo(3009);
    }

    @DisplayName("SkillEventsEntity 리스트를 SkillEvents 도메인 리스트로 변환한다")
    @Test
    void toDomainSkillEventsList_entityList_returnsDomainList() {
        // given
        SkillEventsEntity entity1 = new SkillEventsEntity();
        entity1.setParticipantId(1);
        entity1.setSkillSlot(1);
        entity1.setTimestamp(30000L);

        SkillEventsEntity entity2 = new SkillEventsEntity();
        entity2.setParticipantId(1);
        entity2.setSkillSlot(2);
        entity2.setTimestamp(60000L);

        List<SkillEventsEntity> entities = List.of(entity1, entity2);

        // when
        List<SkillEvents> result = matchMapper.toDomainSkillEventsList(entities);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSkillSlot()).isEqualTo(1);
        assertThat(result.get(1).getSkillSlot()).isEqualTo(2);
    }

    @DisplayName("PerkStyleValue를 Style 도메인으로 변환한다")
    @Test
    void toDomain_perkStyleValue_returnsStyle() {
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
        Style result = matchMapper.toDomain(perkStyleValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryStyleId()).isEqualTo(8000);
        assertThat(result.getPrimaryPerk0()).isEqualTo(8005);
        assertThat(result.getSubStyleId()).isEqualTo(8100);
        assertThat(result.getSubPerk0()).isEqualTo(8139);
    }

    @DisplayName("ItemValue 엔티티를 도메인으로 변환한다")
    @Test
    void toDomain_itemValue_returnsDomainItemValue() {
        // given
        com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue entityItemValue =
                com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue.builder()
                        .item0(3006)
                        .item1(3009)
                        .item2(3047)
                        .item3(3071)
                        .item4(3153)
                        .item5(3508)
                        .item6(3340)
                        .build();

        // when
        ItemValue result = matchMapper.toDomain(entityItemValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItem0()).isEqualTo(3006);
        assertThat(result.getItem1()).isEqualTo(3009);
        assertThat(result.getItem6()).isEqualTo(3340);
    }

    @DisplayName("ItemValue 도메인을 영속성 객체로 변환한다")
    @Test
    void toPersistence_itemValue_returnsEntityItemValue() {
        // given
        ItemValue domainItemValue = ItemValue.builder()
                .item0(3006)
                .item1(3009)
                .item2(3047)
                .item3(3071)
                .item4(3153)
                .item5(3508)
                .item6(3340)
                .build();

        // when
        com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue result =
                matchMapper.toPersistence(domainItemValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItem0()).isEqualTo(3006);
        assertThat(result.getItem1()).isEqualTo(3009);
        assertThat(result.getItem6()).isEqualTo(3340);
    }

    @DisplayName("PerkStatValue 엔티티를 도메인 StatValue로 변환한다")
    @Test
    void toDomain_perkStatValue_returnsDomainStatValue() {
        // given
        PerkStatValue perkStatValue = PerkStatValue.builder()
                .statPerkDefense(5002)
                .statPerkFlex(5008)
                .statPerkOffense(5005)
                .build();

        // when
        StatValue result = matchMapper.toDomain(perkStatValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDefense()).isEqualTo(5002);
        assertThat(result.getFlex()).isEqualTo(5008);
        assertThat(result.getOffense()).isEqualTo(5005);
    }

    @DisplayName("MatchEntity를 Match 도메인으로 변환한다")
    @Test
    void toDomain_matchEntity_returnsMatch() {
        // given
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12345")
                .queueId(420)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .gameType("MATCHED_GAME")
                .gameVersion("14.1.1")
                .season(14)
                .build();

        // when
        var result = matchMapper.toDomain(matchEntity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo("KR_12345");
        assertThat(result.getQueueId()).isEqualTo(420);
    }

    @DisplayName("Match 도메인을 MatchEntity로 변환한다")
    @Test
    void toEntity_match_returnsMatchEntity() {
        // given
        var match = com.example.lolserver.domain.match.domain.Match.builder()
                .matchId("KR_12345")
                .queueId(420)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .build();

        // when
        MatchEntity result = matchMapper.toEntity(match);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo("KR_12345");
        assertThat(result.getQueueId()).isEqualTo(420);
    }
}
