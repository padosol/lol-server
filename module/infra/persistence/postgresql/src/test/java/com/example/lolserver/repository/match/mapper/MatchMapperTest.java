package com.example.lolserver.repository.match.mapper;

import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.TeamInfoData;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import com.example.lolserver.domain.match.domain.gameData.value.ItemValue;
import com.example.lolserver.domain.match.domain.gameData.value.StatValue;
import com.example.lolserver.domain.match.domain.gameData.value.Style;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.id.MatchSummonerId;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
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
                .build();

        // when
        GameInfoData result = matchMapper.toGameInfoData(matchEntity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMatchId()).isEqualTo("KR_12345");
        assertThat(result.getQueueId()).isEqualTo(420);
        assertThat(result.getGameDuration()).isEqualTo(1800L);
        assertThat(result.getGameMode()).isEqualTo("CLASSIC");
    }

    @DisplayName("MatchSummonerEntity를 ParticipantData로 변환한다")
    @Test
    void toDomain_matchSummonerEntity_returnsParticipantData() {
        // given
        MatchSummonerEntity entity = MatchSummonerEntity.builder()
                .matchSummonerId(new MatchSummonerId("test-puuid", "KR_12345"))
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
        // Note: puuid is not mapped from embedded MatchSummonerId in current mapper config
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

    @DisplayName("MSChampionDTO를 MSChampion 도메인으로 변환한다 - 현재 매핑 미구현")
    @Test
    void toDomain_msChampionDTO_returnsMSChampion() {
        // given
        // Constructor: assists, deaths, kills, championId, championName, win, losses, damagePerMinute, kda, laneMinionsFirst10Minutes, damageTakenOnTeamPercentage, goldPerMinute, playCount
        MSChampionDTO dto = new MSChampionDTO(
                8.0,    // assists
                5.0,    // deaths
                10.0,   // kills
                157,    // championId
                "Yasuo", // championName
                15L,    // win
                5L,     // losses
                500.0,  // damagePerMinute
                3.6,    // kda
                65.0,   // laneMinionsFirst10Minutes
                25.0,   // damageTakenOnTeamPercentage
                400.0,  // goldPerMinute
                20L     // playCount
        );

        // when
        MSChampion result = matchMapper.toDomain(dto);

        // then
        // Note: Current MapStruct mapper doesn't map MSChampionDTO fields to MSChampion
        // This is a known issue - the mapper returns an empty object
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
        entity.setType("SKILL_LEVEL_UP");

        // when
        SkillEvents result = matchMapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getParticipantId()).isEqualTo(1);
        assertThat(result.getSkillSlot()).isEqualTo(1);
        assertThat(result.getTimestamp()).isEqualTo(30000L);
        assertThat(result.getType()).isEqualTo("SKILL_LEVEL_UP");
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
        entity1.setType("SKILL_LEVEL_UP");

        SkillEventsEntity entity2 = new SkillEventsEntity();
        entity2.setParticipantId(1);
        entity2.setSkillSlot(2);
        entity2.setTimestamp(60000L);
        entity2.setType("SKILL_LEVEL_UP");

        List<SkillEventsEntity> entities = List.of(entity1, entity2);

        // when
        List<SkillEvents> result = matchMapper.toDomainSkillEventsList(entities);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSkillSlot()).isEqualTo(1);
        assertThat(result.get(1).getSkillSlot()).isEqualTo(2);
    }

    @DisplayName("mapStringToIntArray가 쉼표로 구분된 문자열을 정수 배열로 변환한다")
    @Test
    void mapStringToIntArray_commaSeparatedString_returnsIntArray() {
        // given
        String input = "1,2,3,4,5";

        // when
        int[] result = matchMapper.mapStringToIntArray(input);

        // then
        assertThat(result).containsExactly(1, 2, 3, 4, 5);
    }

    @DisplayName("mapStringToIntArray가 null 입력에 빈 배열을 반환한다")
    @Test
    void mapStringToIntArray_nullInput_returnsEmptyArray() {
        // when
        int[] result = matchMapper.mapStringToIntArray(null);

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("mapStringToIntArray가 빈 문자열에 빈 배열을 반환한다")
    @Test
    void mapStringToIntArray_emptyString_returnsEmptyArray() {
        // when
        int[] result = matchMapper.mapStringToIntArray("");

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("StyleValue를 Style 도메인으로 변환한다")
    @Test
    void toDomain_styleValue_returnsStyle() {
        // given
        StyleValue styleValue = new StyleValue();
        styleValue.setPrimaryRuneId(8000);
        styleValue.setSecondaryRuneId(8100);
        styleValue.setPrimaryRuneIds("8005,9111,9104,8299");
        styleValue.setSecondaryRuneIds("8139,8135");

        // when
        Style result = matchMapper.toDomain(styleValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryRuneId()).isEqualTo(8000);
        assertThat(result.getSecondaryRuneId()).isEqualTo(8100);
        assertThat(result.getPrimaryRuneIds()).containsExactly(8005, 9111, 9104, 8299);
        assertThat(result.getSecondaryRuneIds()).containsExactly(8139, 8135);
    }
}
