package com.example.lolserver.web.match.repository.match.dsl;

import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.entity.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.lolserver.web.match.entity.QMatch.match;
import static com.example.lolserver.web.match.entity.QMatchSummoner.matchSummoner;

@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<Match> getMatches(MatchRequest matchRequest, Pageable pageable) {

        List<Match> result = jpaQueryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).on(matchSummoner.puuid.eq(matchRequest.getPuuid()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .where(queueIdEq(matchRequest.getQueueId()))
                .orderBy(match.gameEndTimestamp.desc())
                .fetch();


        JPAQuery<Match> countQuery = jpaQueryFactory.selectFrom(match)
                .join(match.matchSummoners, matchSummoner).on(matchSummoner.puuid.eq(matchRequest.getPuuid()));

        return PageableExecutionUtils.getPage(result, pageable, () ->  countQuery.fetch().size());
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        if(queueId != null) {
            return match.queueId.eq(queueId);
        }

        return null;
    }

    @Override
    public List<Match> getAllMatches() {
        QMatch match = QMatch.match;
        QMatchSummoner matchSummoner = QMatchSummoner.matchSummoner;

        return jpaQueryFactory.selectFrom(match).fetch();
    }

    @Override
    public List<String> getMatchIdsNotIn(List<String> matchIds) {

        List<String> inMatchIds = jpaQueryFactory.selectFrom(match)
                .where(match.matchId.in(matchIds))
                .fetch()
                .stream().map(Match::getMatchId).toList();

        List<String> result = matchIds.stream().filter(matchId -> {
            return !inMatchIds.contains(matchId);
        }).toList();

        return result;
    }

    @Override
    public void matchBulkInsert(List<Match> matchList) {

        List<MatchSummoner> totalMatchSummoner = new ArrayList<>();
        List<MatchTeam> totalMatchTeams = new ArrayList<>();
        List<Match> totalMatch = new ArrayList<>();

        for (Match match : matchList) {

            totalMatch.add(match);

            if(match.isAbortUnexpected()) {
                continue;
            }

            List<MatchSummoner> matchSummoners = match.getMatchSummoners();
            totalMatchSummoner.addAll(matchSummoners);

            List<MatchTeam> matchTeams = match.getMatchTeams();
            totalMatchTeams.addAll(matchTeams);

        }

        bulkInsertMatch(totalMatch);
        bulkInsertMatchSummoner(totalMatchSummoner);
        bulkInsertMatchTeam(totalMatchTeams);
    }

    private void bulkInsertMatch(List<Match> matchList) {
        jdbcTemplate.batchUpdate("INSERT INTO \"match\" (" +
                        "   map_id," +
                        "   queue_id," +
                        "   season," +
                        "   game_creation," +
                        "   game_duration," +
                        "   game_end_timestamp," +
                        "   game_id," +
                        "   game_start_timestamp," +
                        "   date_version," +
                        "   end_of_game_result," +
                        "   game_mode," +
                        "   game_name," +
                        "   game_type," +
                        "   game_version," +
                        "   match_id," +
                        "   platform_id," +
                        "   tournament_code,"+
                        "   game_create_datetime," +
                        "   game_end_datetime," +
                        "   game_start_datetime" +
                        ")" +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (match_id) DO NOTHING",
                matchList,
                100,
                (PreparedStatement ps, Match match) -> {
                    ps.setInt(1, match.getMapId());
                    ps.setInt(2, match.getQueueId());
                    ps.setInt(3, match.getSeason());
                    ps.setLong(4, match.getGameCreation());
                    ps.setLong(5, match.getGameDuration());
                    ps.setLong(6, match.getGameEndTimestamp());
                    ps.setLong(7, match.getGameId());
                    ps.setLong(8, match.getGameStartTimestamp());
                    ps.setString(9, match.getDateVersion());
                    ps.setString(10, match.getEndOfGameResult());
                    ps.setString(11, match.getGameMode());
                    ps.setString(12, match.getGameName());
                    ps.setString(13, match.getGameType());
                    ps.setString(14, match.getGameVersion());
                    ps.setString(15, match.getMatchId());
                    ps.setString(16, match.getPlatformId());
                    ps.setString(17, match.getTournamentCode());
                    ps.setTimestamp(18, Timestamp.valueOf(match.getGameCreateDatetime()));
                    ps.setTimestamp(19, Timestamp.valueOf(match.getGameEndDatetime()));
                    ps.setTimestamp(20, Timestamp.valueOf(match.getGameStartDatetime()));
                }
        );
    }


    private void bulkInsertMatchSummoner(List<MatchSummoner> matchSummoners) {

        jdbcTemplate.batchUpdate(
                "INSERT INTO match_summoner (" +
                        "   assists," +
                        "   baron_kills," +
                        "   bounty_level," +
                        "   champ_experience," +
                        "   champ_level," +
                        "   champion_id," +
                        "   champion_transform," +
                        "   consumables_purchased," +
                        "   damage_dealt_to_buildings," +
                        "   damage_dealt_to_objectives," +
                        "   damage_dealt_to_turrets," +
                        "   damage_self_mitigated," +
                        "   deaths," +
                        "   defense," +
                        "   detector_wards_placed," +
                        "   double_kills," +
                        "   dragon_kills," +
                        "   first_blood_assist," +
                        "   first_blood_kill," +
                        "   first_tower_assist," +
                        "   first_tower_kill," +
                        "   flex," +
                        "   game_ended_in_early_surrender," +
                        "   game_ended_in_surrender," +
                        "   gold_earned," +
                        "   gold_spent," +
                        "   inhibitor_kills," +
                        "   inhibitor_takedowns," +
                        "   inhibitors_lost," +
                        "   item0," +
                        "   item1," +
                        "   item2," +
                        "   item3," +
                        "   item4," +
                        "   item5," +
                        "   item6," +
                        "   items_purchased," +
                        "   killing_sprees," +
                        "   kills," +
                        "   largest_critical_strike," +
                        "   largest_killing_spree," +
                        "   largest_multi_kill," +
                        "   longest_time_spent_living," +
                        "   magic_damage_dealt," +
                        "   magic_damage_dealt_to_champions," +
                        "   magic_damage_taken," +
                        "   neutral_minions_killed," +
                        "   nexus_kills," +
                        "   nexus_lost," +
                        "   nexus_takedowns," +
                        "   objectives_stolen," +
                        "   objectives_stolen_assists," +
                        "   offense," +
                        "   participant_id," +
                        "   penta_kills," +
                        "   physical_damage_dealt," +
                        "   physical_damage_dealt_to_champions," +
                        "   physical_damage_taken," +
                        "   primary_rune_id," +
                        "   profile_icon," +
                        "   quadra_kills," +
                        "   secondary_rune_id," +
                        "   sight_wards_bought_in_game," +
                        "   spell1casts," +
                        "   spell2casts," +
                        "   spell3casts," +
                        "   spell4casts," +
                        "   summoner1casts," +
                        "   summoner1id," +
                        "   summoner2casts," +
                        "   summoner2id," +
                        "   summoner_level," +
                        "   team_early_surrendered," +
                        "   team_id," +
                        "   time_played," +
                        "   timeccing_others," +
                        "   total_damage_dealt," +
                        "   total_damage_dealt_to_champions," +
                        "   total_damage_shielded_on_teammates," +
                        "   total_damage_taken," +
                        "   total_heal," +
                        "   total_heals_on_teammates," +
                        "   total_minions_killed," +
                        "   total_time_spent_dead," +
                        "   total_timeccdealt," +
                        "   total_units_healed," +
                        "   triple_kills," +
                        "   true_damage_dealt," +
                        "   true_damage_dealt_to_champions," +
                        "   true_damage_taken," +
                        "   turret_kills," +
                        "   turret_takedowns," +
                        "   turrets_lost," +
                        "   unreal_kills," +
                        "   vision_score," +
                        "   vision_wards_bought_in_game," +
                        "   wards_killed," +
                        "   wards_placed," +
                        "   win," +
                        "   champion_name," +
                        "   individual_position," +
                        "   lane," +
                        "   match_id," +
                        "   primary_rune_ids," +
                        "   puuid," +
                        "   riot_id_game_name," +
                        "   riot_id_tagline," +
                        "   role," +
                        "   secondary_rune_ids," +
                        "   summoner_id," +
                        "   summoner_name," +
                        "   team_position"+
                        ")" +
                        "VALUES (" +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?," +
                        "?"+
                        ") ON CONFLICT (match_id, summoner_id) DO NOTHING",
                matchSummoners,
                100,
                (PreparedStatement ps, MatchSummoner matchSummoner) -> {
                    ps.setInt(1, matchSummoner.getAssists());
                    ps.setInt(2, matchSummoner.getBaronKills());
                    ps.setInt(3, matchSummoner.getBountyLevel());
                    ps.setInt(4, matchSummoner.getChampExperience());
                    ps.setInt(5, matchSummoner.getChampLevel());
                    ps.setInt(6, matchSummoner.getChampionId());
                    ps.setInt(7, matchSummoner.getChampionTransform());
                    ps.setInt(8, matchSummoner.getConsumablesPurchased());
                    ps.setInt(9, matchSummoner.getDamageDealtToBuildings());
                    ps.setInt(10, matchSummoner.getDamageDealtToObjectives());
                    ps.setInt(11, matchSummoner.getDamageDealtToTurrets());
                    ps.setInt(12, matchSummoner.getDamageSelfMitigated());
                    ps.setInt(13, matchSummoner.getDeaths());
                    ps.setInt(14, matchSummoner.getStatValue().getDefense());
                    ps.setInt(15, matchSummoner.getDetectorWardsPlaced());
                    ps.setInt(16, matchSummoner.getDoubleKills());
                    ps.setInt(17, matchSummoner.getDragonKills());
                    ps.setBoolean(18, matchSummoner.isFirstBloodAssist());
                    ps.setBoolean(19, matchSummoner.isFirstBloodKill());
                    ps.setBoolean(20, matchSummoner.isFirstTowerAssist());
                    ps.setBoolean(21, matchSummoner.isFirstTowerKill());
                    ps.setInt(22, matchSummoner.getStatValue().getFlex());
                    ps.setBoolean(23, matchSummoner.isGameEndedInEarlySurrender());
                    ps.setBoolean(24, matchSummoner.isGameEndedInSurrender());
                    ps.setInt(25, matchSummoner.getGoldEarned());
                    ps.setInt(26, matchSummoner.getGoldSpent());
                    ps.setInt(27, matchSummoner.getInhibitorKills());
                    ps.setInt(28, matchSummoner.getInhibitorTakedowns());
                    ps.setInt(29, matchSummoner.getInhibitorsLost());
                    ps.setInt(30, matchSummoner.getItem().getItem0());
                    ps.setInt(31, matchSummoner.getItem().getItem1());
                    ps.setInt(32, matchSummoner.getItem().getItem2());
                    ps.setInt(33, matchSummoner.getItem().getItem3());
                    ps.setInt(34, matchSummoner.getItem().getItem4());
                    ps.setInt(35, matchSummoner.getItem().getItem5());
                    ps.setInt(36, matchSummoner.getItem().getItem6());
                    ps.setInt(37, matchSummoner.getItemsPurchased());
                    ps.setInt(38, matchSummoner.getKillingSprees());
                    ps.setInt(39, matchSummoner.getKills());
                    ps.setInt(40, matchSummoner.getLargestCriticalStrike());
                    ps.setInt(41, matchSummoner.getLargestKillingSpree());
                    ps.setInt(42, matchSummoner.getLargestMultiKill());
                    ps.setInt(43, matchSummoner.getLongestTimeSpentLiving());
                    ps.setInt(44, matchSummoner.getMagicDamageDealt());
                    ps.setInt(45, matchSummoner.getMagicDamageDealtToChampions());
                    ps.setInt(46, matchSummoner.getMagicDamageTaken());
                    ps.setInt(47, matchSummoner.getNeutralMinionsKilled());
                    ps.setInt(48, matchSummoner.getNexusKills());
                    ps.setInt(49, matchSummoner.getNexusLost());
                    ps.setInt(50, matchSummoner.getNexusTakedowns());
                    ps.setInt(51, matchSummoner.getObjectivesStolen());
                    ps.setInt(52, matchSummoner.getObjectivesStolenAssists());
                    ps.setInt(53, matchSummoner.getStatValue().getOffense());
                    ps.setInt(54, matchSummoner.getParticipantId());
                    ps.setInt(55, matchSummoner.getPentaKills());
                    ps.setInt(56, matchSummoner.getPhysicalDamageDealt());
                    ps.setInt(57, matchSummoner.getPhysicalDamageDealtToChampions());
                    ps.setInt(58, matchSummoner.getPhysicalDamageTaken());
                    ps.setInt(59, matchSummoner.getStyleValue().getPrimaryRuneId());
                    ps.setInt(60, matchSummoner.getProfileIcon());
                    ps.setInt(61, matchSummoner.getQuadraKills());
                    ps.setInt(62, matchSummoner.getStyleValue().getSecondaryRuneId());
                    ps.setInt(63, matchSummoner.getSightWardsBoughtInGame());
                    ps.setInt(64, matchSummoner.getSpell1Casts());
                    ps.setInt(65, matchSummoner.getSpell2Casts());
                    ps.setInt(66, matchSummoner.getSpell3Casts());
                    ps.setInt(67, matchSummoner.getSpell4Casts());
                    ps.setInt(68, matchSummoner.getSummoner1Casts());
                    ps.setInt(69, matchSummoner.getSummoner1Id());
                    ps.setInt(70, matchSummoner.getSummoner2Casts());
                    ps.setInt(71, matchSummoner.getSummoner2Id());
                    ps.setInt(72, matchSummoner.getSummonerLevel());
                    ps.setBoolean(73, matchSummoner.isTeamEarlySurrendered());
                    ps.setInt(74, matchSummoner.getTeamId());
                    ps.setInt(75, matchSummoner.getTimePlayed());
                    ps.setInt(76, matchSummoner.getTotalTimeCCDealt());
                    ps.setInt(77, matchSummoner.getTotalDamageDealt());
                    ps.setInt(78, matchSummoner.getTotalDamageDealtToChampions());
                    ps.setInt(79, matchSummoner.getTotalDamageShieldedOnTeammates());
                    ps.setInt(80, matchSummoner.getTotalDamageTaken());
                    ps.setInt(81, matchSummoner.getTotalHeal());
                    ps.setInt(82, matchSummoner.getTotalHealsOnTeammates());
                    ps.setInt(83, matchSummoner.getTotalMinionsKilled());
                    ps.setInt(84, matchSummoner.getTotalTimeSpentDead());
                    ps.setInt(85, matchSummoner.getTotalTimeCCDealt());
                    ps.setInt(86, matchSummoner.getTotalUnitsHealed());
                    ps.setInt(87, matchSummoner.getTripleKills());
                    ps.setInt(88, matchSummoner.getTrueDamageDealt());
                    ps.setInt(89, matchSummoner.getTrueDamageDealtToChampions());
                    ps.setInt(90, matchSummoner.getTrueDamageTaken());
                    ps.setInt(91, matchSummoner.getTurretKills());
                    ps.setInt(92, matchSummoner.getTurretTakedowns());
                    ps.setInt(93, matchSummoner.getTurretsLost());
                    ps.setInt(94, matchSummoner.getUnrealKills());
                    ps.setInt(95, matchSummoner.getVisionScore());
                    ps.setInt(96, matchSummoner.getVisionWardsBoughtInGame());
                    ps.setInt(97, matchSummoner.getWardsKilled());
                    ps.setInt(98, matchSummoner.getWardsPlaced());
                    ps.setBoolean(99, matchSummoner.isWin());
                    ps.setString(100, matchSummoner.getChampionName());
                    ps.setString(101, matchSummoner.getIndividualPosition());
                    ps.setString(102, matchSummoner.getLane());
                    ps.setString(103, matchSummoner.getMatch().getMatchId());
                    ps.setInt(104, matchSummoner.getStyleValue().getPrimaryRuneId());
                    ps.setString(105, matchSummoner.getPuuid());
                    ps.setString(106, matchSummoner.getRiotIdGameName());
                    ps.setString(107, matchSummoner.getRiotIdTagline());
                    ps.setString(108, matchSummoner.getRole());
                    ps.setInt(109, matchSummoner.getStyleValue().getSecondaryRuneId());
                    ps.setString(110, matchSummoner.getSummonerId());
                    ps.setString(111, matchSummoner.getSummonerName());
                    ps.setString(112, matchSummoner.getTeamPosition());
                }
        );

    }


    private void bulkInsertMatchTeam(List<MatchTeam> matchTeams) {

        jdbcTemplate.batchUpdate(
                "INSERT INTO match_team (" +
                        "   baron_first," +
                        "   baron_kills," +
                        "   champion1id," +
                        "   champion2id," +
                        "   champion3id," +
                        "   champion4id," +
                        "   champion5id," +
                        "   champion_first," +
                        "   champion_kills," +
                        "   dragon_first," +
                        "   dragon_kills," +
                        "   inhibitor_first," +
                        "   inhibitor_kills," +
                        "   pick1turn," +
                        "   pick2turn," +
                        "   pick3turn," +
                        "   pick4turn," +
                        "   pick5turn," +
                        "   rift_herald_first," +
                        "   rift_herald_kills," +
                        "   team_id," +
                        "   tower_first," +
                        "   tower_kills," +
                        "   win," +
                        "   match_id"+
                        ")" +
                        "VALUES (" +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?," +
                        "   ?"+
                        ") ON CONFLICT (match_id, team_id) DO NOTHING",
                matchTeams,
                100,
                (PreparedStatement ps, MatchTeam matchTeam) -> {
                    ps.setBoolean(1, matchTeam.getTeamObject().isBaronFirst());
                    ps.setInt(2, matchTeam.getTeamObject().getBaronKills());
                    ps.setInt(3, matchTeam.getTeamBan().getChampion1Id());
                    ps.setInt(4, matchTeam.getTeamBan().getChampion2Id());
                    ps.setInt(5, matchTeam.getTeamBan().getChampion3Id());
                    ps.setInt(6, matchTeam.getTeamBan().getChampion4Id());
                    ps.setInt(7, matchTeam.getTeamBan().getChampion5Id());
                    ps.setBoolean(8, matchTeam.getTeamObject().isChampionFirst());
                    ps.setInt(9, matchTeam.getTeamObject().getChampionKills());
                    ps.setBoolean(10, matchTeam.getTeamObject().isDragonFirst());
                    ps.setInt(11, matchTeam.getTeamObject().getChampionKills());
                    ps.setBoolean(12, matchTeam.getTeamObject().isInhibitorFirst());
                    ps.setInt(13, matchTeam.getTeamObject().getInhibitorKills());
                    ps.setInt(14, matchTeam.getTeamBan().getPick1Turn());
                    ps.setInt(15, matchTeam.getTeamBan().getPick2Turn());
                    ps.setInt(16, matchTeam.getTeamBan().getPick3Turn());
                    ps.setInt(17, matchTeam.getTeamBan().getPick4Turn());
                    ps.setInt(18, matchTeam.getTeamBan().getPick5Turn());
                    ps.setBoolean(19, matchTeam.getTeamObject().isRiftHeraldFirst());
                    ps.setInt(20, matchTeam.getTeamObject().getRiftHeraldKills());
                    ps.setInt(21, matchTeam.getTeamId());
                    ps.setBoolean(22, matchTeam.getTeamObject().isTowerFirst());
                    ps.setInt(23, matchTeam.getTeamObject().getTowerKills());
                    ps.setBoolean(24, matchTeam.isWin());
                    ps.setString(25, matchTeam.getMatch().getMatchId());
                }

        );


    }



}
