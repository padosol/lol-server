package com.example.lolserver.repository.match;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchteam.MatchTeamRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MatchRepositoryIntegrationTest extends RepositoryTestBase {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchSummonerRepository matchSummonerRepository;

    @Autowired
    private MatchTeamRepository matchTeamRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("MatchEntity를 저장하고 조회할 수 있다")
    @Test
    void saveAndFindMatch_validEntity_success() {
        // given
        MatchEntity match = MatchEntity.builder()
                .matchId("KR_TEST_12345")
                .dataVersion("2")
                .endOfGameResult("GameComplete")
                .gameCreation(1700000000000L)
                .gameDuration(1800L)
                .gameEndTimestamp(1700001800000L)
                .gameStartTimestamp(1700000000000L)
                .gameId(12345L)
                .gameMode("CLASSIC")
                .gameName("Team1 vs Team2")
                .gameType("MATCHED_GAME")
                .gameVersion("14.1.1")
                .mapId(11)
                .queueId(420)
                .platformId("KR")
                .tournamentCode("")
                .season(14)
                .gameCreateDatetime(LocalDateTime.now())
                .gameEndDatetime(LocalDateTime.now())
                .gameStartDatetime(LocalDateTime.now())
                .build();

        // when
        matchRepository.save(match);
        entityManager.flush();
        entityManager.clear();

        Optional<MatchEntity> found = matchRepository.findById("KR_TEST_12345");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getMatchId()).isEqualTo("KR_TEST_12345");
        assertThat(found.get().getQueueId()).isEqualTo(420);
        assertThat(found.get().getGameMode()).isEqualTo("CLASSIC");
        assertThat(found.get().getSeason()).isEqualTo(14);
    }

    @DisplayName("MatchSummonerEntity를 저장하고 조회할 수 있다")
    @Test
    void saveAndFindMatchSummoner_validEntity_success() {
        // given
        MatchEntity match = MatchEntity.builder()
                .matchId("KR_SUMMONER_TEST")
                .queueId(420)
                .gameMode("CLASSIC")
                .gameDuration(1800L)
                .build();
        matchRepository.save(match);

        MatchSummonerEntity summoner = MatchSummonerEntity.builder()
                .puuid("test-puuid-1")
                .matchId("KR_SUMMONER_TEST")
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .champLevel(18)
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .role("SUPPORT")
                .lane("BOTTOM")
                .teamPosition("UTILITY")
                .individualPosition("SUPPORT")
                .totalDamageDealtToChampions(25000)
                .goldEarned(15000)
                .neutralMinionsKilled(50)
                .totalMinionsKilled(200)
                .damageDealtToBuildings(5000)
                .placement(1)
                .gameEndedInEarlySurrender(false)
                .gameEndedInSurrender(false)
                .build();

        // when
        matchSummonerRepository.save(summoner);
        entityManager.flush();
        entityManager.clear();

        List<MatchSummonerEntity> foundList = matchSummonerRepository.findByMatchId("KR_SUMMONER_TEST");

        // then
        assertThat(foundList).isNotEmpty();
        MatchSummonerEntity found = foundList.stream()
                .filter(e -> e.getPuuid().equals("test-puuid-1"))
                .findFirst()
                .orElseThrow();
        assertThat(found.getChampionName()).isEqualTo("Yasuo");
        assertThat(found.getKills()).isEqualTo(10);
        assertThat(found.isWin()).isTrue();
    }

    @DisplayName("MatchSummonerEntity에 Value Objects를 저장할 수 있다")
    @Test
    void saveMatchSummonerWithValueObjects_validEntity_success() {
        // given
        MatchEntity match = MatchEntity.builder()
                .matchId("KR_VALUE_TEST")
                .queueId(420)
                .gameMode("CLASSIC")
                .gameDuration(1800L)
                .build();
        matchRepository.save(match);

        ItemValue itemValue = ItemValue.builder()
                .item0(3006)
                .item1(3009)
                .item2(3153)
                .item3(3072)
                .item4(3036)
                .item5(3033)
                .item6(3340)
                .build();

        StatValue statValue = StatValue.builder()
                .defense(50)
                .flex(30)
                .offense(40)
                .build();

        StyleValue styleValue = StyleValue.builder()
                .primaryRuneId(8000)
                .secondaryRuneId(8100)
                .primaryRuneIds("8005,9111,9104,8299")
                .secondaryRuneIds("8139,8135")
                .build();

        MatchSummonerEntity summoner = MatchSummonerEntity.builder()
                .puuid("test-puuid-value")
                .matchId("KR_VALUE_TEST")
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .item(itemValue)
                .statValue(statValue)
                .styleValue(styleValue)
                .build();

        // when
        matchSummonerRepository.save(summoner);
        entityManager.flush();
        entityManager.clear();

        List<MatchSummonerEntity> foundList = matchSummonerRepository.findByMatchId("KR_VALUE_TEST");

        // then
        assertThat(foundList).isNotEmpty();
        MatchSummonerEntity found = foundList.stream()
                .filter(e -> e.getPuuid().equals("test-puuid-value"))
                .findFirst()
                .orElseThrow();
        assertThat(found.getItem().getItem0()).isEqualTo(3006);
        assertThat(found.getStatValue().getDefense()).isEqualTo(50);
        assertThat(found.getStyleValue().getPrimaryRuneId()).isEqualTo(8000);
    }

    @DisplayName("MatchTeamEntity를 저장하고 조회할 수 있다")
    @Test
    void saveAndFindMatchTeam_validEntity_success() {
        // given
        MatchTeamEntity team = MatchTeamEntity.builder()
                .matchId("KR_TEAM_TEST")
                .teamId(100)
                .win(true)
                .build();

        // when
        matchTeamRepository.save(team);
        entityManager.flush();
        entityManager.clear();

        List<MatchTeamEntity> found = matchTeamRepository.findAll();

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.get(found.size() - 1).getTeamId()).isEqualTo(100);
        assertThat(found.get(found.size() - 1).isWin()).isTrue();
    }

    @DisplayName("MatchSummonerEntity의 모든 필드를 사용할 수 있다")
    @Test
    void matchSummonerEntity_allFields_accessible() {
        // given
        MatchEntity match = MatchEntity.builder()
                .matchId("KR_ALL_FIELDS")
                .queueId(420)
                .gameMode("CLASSIC")
                .gameDuration(1800L)
                .build();
        matchRepository.save(match);

        MatchSummonerEntity summoner = MatchSummonerEntity.builder()
                .puuid("test-puuid-all")
                .matchId("KR_ALL_FIELDS")
                .summonerId("summ-id-123")
                .riotIdGameName("TestPlayer")
                .riotIdTagline("KR1")
                .profileIcon(5001)
                .summonerName("TestPlayer")
                .participantId(1)
                .champLevel(18)
                .championId(157)
                .championName("Yasuo")
                .lane("MIDDLE")
                .champExperience(25000)
                .role("SOLO")
                .spell1Casts(50)
                .spell2Casts(30)
                .spell3Casts(100)
                .spell4Casts(40)
                .summoner1Casts(10)
                .summoner1Id(4)
                .summoner2Casts(5)
                .summoner2Id(12)
                .summonerLevel(500)
                .bountyLevel(2)
                .kills(15)
                .assists(10)
                .deaths(3)
                .doubleKills(3)
                .tripleKills(1)
                .quadraKills(0)
                .pentaKills(0)
                .unrealKills(0)
                .championTransform(0)
                .goldEarned(18000)
                .goldSpent(17000)
                .itemsPurchased(15)
                .consumablesPurchased(3)
                .neutralMinionsKilled(100)
                .totalMinionsKilled(250)
                .objectivesStolen(1)
                .objectivesStolenAssists(2)
                .detectorWardsPlaced(5)
                .sightWardsBoughtInGame(3)
                .visionScore(45)
                .visionWardsBoughtInGame(2)
                .wardsKilled(8)
                .wardsPlaced(15)
                .baronKills(1)
                .dragonKills(3)
                .firstBloodAssist(false)
                .firstBloodKill(true)
                .firstTowerAssist(true)
                .firstTowerKill(false)
                .inhibitorKills(2)
                .inhibitorTakedowns(3)
                .inhibitorsLost(1)
                .nexusKills(1)
                .nexusTakedowns(1)
                .nexusLost(0)
                .turretKills(4)
                .turretTakedowns(6)
                .turretsLost(2)
                .gameEndedInEarlySurrender(false)
                .gameEndedInSurrender(false)
                .teamEarlySurrendered(false)
                .teamPosition("MIDDLE")
                .teamId(100)
                .win(true)
                .timePlayed(1800)
                .individualPosition("MIDDLE")
                .magicDamageDealt(50000)
                .magicDamageDealtToChampions(30000)
                .magicDamageTaken(15000)
                .physicalDamageDealt(20000)
                .physicalDamageDealtToChampions(15000)
                .physicalDamageTaken(10000)
                .damageDealtToBuildings(8000)
                .damageDealtToObjectives(25000)
                .damageDealtToTurrets(7000)
                .damageSelfMitigated(12000)
                .totalDamageDealt(100000)
                .totalDamageDealtToChampions(50000)
                .totalDamageShieldedOnTeammates(5000)
                .totalDamageTaken(30000)
                .trueDamageDealt(5000)
                .trueDamageDealtToChampions(3000)
                .trueDamageTaken(2000)
                .totalHeal(8000)
                .totalHealsOnTeammates(3000)
                .totalTimeCCDealt(120)
                .totalTimeSpentDead(45)
                .totalUnitsHealed(10)
                .timeCCingOthers(80)
                .killingSprees(3)
                .largestCriticalStrike(1500)
                .largestKillingSpree(8)
                .largestMultiKill(3)
                .longestTimeSpentLiving(600)
                .placement(0)
                .build();

        // when
        matchSummonerRepository.save(summoner);
        entityManager.flush();
        entityManager.clear();

        List<MatchSummonerEntity> foundList = matchSummonerRepository.findByMatchId("KR_ALL_FIELDS");

        // then
        assertThat(foundList).isNotEmpty();
        MatchSummonerEntity entity = foundList.stream()
                .filter(e -> e.getPuuid().equals("test-puuid-all"))
                .findFirst()
                .orElseThrow();
        assertThat(entity.getSummonerId()).isEqualTo("summ-id-123");
        assertThat(entity.getRiotIdGameName()).isEqualTo("TestPlayer");
        assertThat(entity.getKills()).isEqualTo(15);
        assertThat(entity.getTotalDamageDealtToChampions()).isEqualTo(50000);
        assertThat(entity.isFirstBloodKill()).isTrue();
        assertThat(entity.getBaronKills()).isEqualTo(1);
    }
}
