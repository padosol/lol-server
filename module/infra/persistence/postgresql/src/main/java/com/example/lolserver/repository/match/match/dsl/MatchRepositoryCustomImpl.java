package com.example.lolserver.repository.match.match.dsl;

import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.QMatchDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStyleValue;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.example.lolserver.support.logging.LogExecutionTime;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.lolserver.repository.match.entity.QMatchSummonerEntity.matchSummonerEntity;
import static com.example.lolserver.repository.match.entity.QMatchEntity.matchEntity;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchRepositoryCustomImpl implements MatchRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;

    @Override
    public Slice<MatchEntity> getMatches(String puuid, Integer queueId,
            Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<MatchEntity> result = jpaQueryFactory.selectFrom(matchEntity)
                .join(matchSummonerEntity)
                .on(matchSummonerEntity.matchId.eq(matchEntity.matchId))
                .where(
                        puuidEq(puuid),
                        queueIdEq(queueId),
                        matchEntity.gameMode.equalsIgnoreCase("CLASSIC")
                                .or(matchEntity.gameMode
                                        .equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchEntity.gameEndTimestamp.desc())
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<MatchEntity> content = hasNext
                ? result.subList(0, pageSize) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression queueIdEq(Integer queueId) {
        if (queueId != null) {
            return matchEntity.queueId.eq(queueId);
        }

        return null;
    }

    private BooleanExpression puuidEq(String puuid) {
        return StringUtils.hasText(puuid)
                ? matchSummonerEntity.puuid.eq(puuid) : null;
    }

    private BooleanExpression seasonEq(Integer season) {
        return season != null ? matchEntity.season.eq(season) : null;
    }

    @LogExecutionTime
    @Override
    public Slice<MatchDTO> getMatchDTOs(
            String puuid, Integer season, Integer queueId, Pageable pageable
    ) {
        int pageSize = pageable.getPageSize();

        List<MatchDTO> result = jpaQueryFactory
                .select(new QMatchDTO(
                        matchEntity.matchId,
                        matchEntity.dataVersion,
                        matchEntity.gameCreation,
                        matchEntity.gameDuration,
                        matchEntity.gameEndTimestamp,
                        matchEntity.gameStartTimestamp,
                        matchEntity.gameMode,
                        matchEntity.gameType,
                        matchEntity.gameVersion,
                        matchEntity.mapId,
                        matchEntity.queueId,
                        matchEntity.platformId,
                        matchEntity.tournamentCode,
                        matchEntity.averageTier
                ))
                .from(matchEntity)
                .join(matchSummonerEntity)
                .on(matchSummonerEntity.matchId.eq(matchEntity.matchId))
                .where(
                        puuidEq(puuid),
                        seasonEq(season),
                        queueIdEq(queueId),
                        matchEntity.gameMode.equalsIgnoreCase("CLASSIC")
                                .or(matchEntity.gameMode
                                        .equalsIgnoreCase("CHERRY"))
                )
                .orderBy(matchEntity.gameEndTimestamp.desc())
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<MatchDTO> content = hasNext
                ? result.subList(0, pageSize) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @LogExecutionTime
    @Override
    public List<MatchSummonerDTO> getMatchSummoners(
            List<String> matchIds
    ) {
        String sql = """
                SELECT mp.puuid, mp.match_id, mp.summoner_id,
                       COALESCE(s.game_name, mp.riot_id_game_name) AS riot_id_game_name,
                       COALESCE(s.tag_line, mp.riot_id_tagline) AS riot_id_tagline,
                       mp.profile_icon, mp.participant_id, mp.tier, mp.tier_rank,
                       mp.absolute_points, mp.summoner_level,
                       mp.champ_level, mp.champion_id, mp.champion_name, mp.champ_experience,
                       mp.summoner1id, mp.summoner2id,
                       mp.kills, mp.assists, mp.deaths,
                       mp.double_kills, mp.triple_kills, mp.quadra_kills, mp.penta_kills,
                       mp.gold_earned, mp.consumables_purchased, mp.items_purchased,
                       mp.neutral_minions_killed, mp.total_minions_killed,
                       mp.vision_score, mp.vision_wards_bought_in_game, mp.wards_killed, mp.wards_placed,
                       mp.total_damage_dealt_to_champions, mp.total_damage_taken,
                       mp.team_id, mp.team_position, mp.win, mp.time_played, mp.timeccing_others,
                       mp.individual_position, mp.lane, mp.role, mp.placement,
                       mp.player_augment1, mp.player_augment2, mp.player_augment3, mp.player_augment4,
                       mp.item0, mp.item1, mp.item2, mp.item3, mp.item4, mp.item5, mp.item6,
                       mp.stat_perk_defense, mp.stat_perk_flex, mp.stat_perk_offense,
                       mp.primary_style_id, mp.primary_perk0, mp.primary_perk1, mp.primary_perk2, mp.primary_perk3,
                       mp.sub_style_id, mp.sub_perk0, mp.sub_perk1,
                       mt.champion_kills AS team_champion_kills,
                       mt.baron_kills AS team_baron_kills,
                       mt.dragon_kills AS team_dragon_kills,
                       mt.tower_kills AS team_tower_kills,
                       mt.inhibitor_kills AS team_inhibitor_kills,
                       pf_agg.gold_timeline,
                       pf_agg.timestamps
                FROM match_participant mp
                LEFT JOIN summoner s ON s.puuid = mp.puuid
                LEFT JOIN match_team mt ON mt.match_id = mp.match_id AND mt.team_id = mp.team_id
                LEFT JOIN (
                    SELECT match_id, participant_id,
                           array_agg(total_gold ORDER BY timestamp) AS gold_timeline,
                           array_agg(timestamp ORDER BY timestamp) AS timestamps
                    FROM participant_frame
                    WHERE match_id IN (:matchIds)
                    GROUP BY match_id, participant_id
                ) pf_agg ON mp.match_id = pf_agg.match_id AND mp.participant_id = pf_agg.participant_id
                WHERE mp.match_id IN (:matchIds)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("matchIds", matchIds)
                .getResultList();

        return rows.stream().map(this::toMatchSummonerDTO).toList();
    }

    @Override
    public List<MatchSummonerDTO> getMatchSummoners(String matchId) {
        return getMatchSummoners(List.of(matchId));
    }

    private MatchSummonerDTO toMatchSummonerDTO(Object[] row) {
        MatchSummonerDTO dto = new MatchSummonerDTO();
        int i = 0;

        dto.setPuuid((String) row[i++]);
        dto.setMatchId((String) row[i++]);
        dto.setSummonerId((String) row[i++]);
        dto.setRiotIdGameName((String) row[i++]);
        dto.setRiotIdTagline((String) row[i++]);
        dto.setProfileIcon(toInt(row[i++]));
        dto.setParticipantId(toInt(row[i++]));
        dto.setTier((String) row[i++]);
        dto.setTierRank((String) row[i++]);
        dto.setAbsolutePoints(row[i] != null ? toInt(row[i]) : null); i++;
        dto.setSummonerLevel(toInt(row[i++]));

        dto.setChampLevel(toInt(row[i++]));
        dto.setChampionId(toInt(row[i++]));
        dto.setChampionName((String) row[i++]);
        dto.setChampExperience(toInt(row[i++]));
        dto.setSummoner1Id(toInt(row[i++]));
        dto.setSummoner2Id(toInt(row[i++]));

        dto.setKills(toInt(row[i++]));
        dto.setAssists(toInt(row[i++]));
        dto.setDeaths(toInt(row[i++]));
        dto.setDoubleKills(toInt(row[i++]));
        dto.setTripleKills(toInt(row[i++]));
        dto.setQuadraKills(toInt(row[i++]));
        dto.setPentaKills(toInt(row[i++]));

        dto.setGoldEarned(toInt(row[i++]));
        dto.setConsumablesPurchased(toInt(row[i++]));
        dto.setItemsPurchased(toInt(row[i++]));
        dto.setNeutralMinionsKilled(toInt(row[i++]));
        dto.setTotalMinionsKilled(toInt(row[i++]));

        dto.setVisionScore(toInt(row[i++]));
        dto.setVisionWardsBoughtInGame(toInt(row[i++]));
        dto.setWardsKilled(toInt(row[i++]));
        dto.setWardsPlaced(toInt(row[i++]));

        dto.setTotalDamageDealtToChampions(toInt(row[i++]));
        dto.setTotalDamageTaken(toInt(row[i++]));

        dto.setTeamId(toInt(row[i++]));
        dto.setTeamPosition((String) row[i++]);
        dto.setWin((Boolean) row[i++]);
        dto.setTimePlayed(toInt(row[i++]));
        dto.setTimeCCingOthers(toInt(row[i++]));
        dto.setIndividualPosition((String) row[i++]);
        dto.setLane((String) row[i++]);
        dto.setRole((String) row[i++]);
        dto.setPlacement(toInt(row[i++]));

        dto.setPlayerAugment1(toInt(row[i++]));
        dto.setPlayerAugment2(toInt(row[i++]));
        dto.setPlayerAugment3(toInt(row[i++]));
        dto.setPlayerAugment4(toInt(row[i++]));

        ItemValue item = new ItemValue();
        item.setItem0(toInt(row[i++]));
        item.setItem1(toInt(row[i++]));
        item.setItem2(toInt(row[i++]));
        item.setItem3(toInt(row[i++]));
        item.setItem4(toInt(row[i++]));
        item.setItem5(toInt(row[i++]));
        item.setItem6(toInt(row[i++]));
        dto.setItem(item);

        PerkStatValue perkStat = new PerkStatValue();
        perkStat.setStatPerkDefense(toInt(row[i++]));
        perkStat.setStatPerkFlex(toInt(row[i++]));
        perkStat.setStatPerkOffense(toInt(row[i++]));
        dto.setPerkStat(perkStat);

        PerkStyleValue perkStyle = new PerkStyleValue();
        perkStyle.setPrimaryStyleId(toInt(row[i++]));
        perkStyle.setPrimaryPerk0(toInt(row[i++]));
        perkStyle.setPrimaryPerk1(toInt(row[i++]));
        perkStyle.setPrimaryPerk2(toInt(row[i++]));
        perkStyle.setPrimaryPerk3(toInt(row[i++]));
        perkStyle.setSubStyleId(toInt(row[i++]));
        perkStyle.setSubPerk0(toInt(row[i++]));
        perkStyle.setSubPerk1(toInt(row[i++]));
        dto.setPerkStyle(perkStyle);

        dto.setTeamChampionKills(toInt(row[i++]));
        dto.setTeamBaronKills(toInt(row[i++]));
        dto.setTeamDragonKills(toInt(row[i++]));
        dto.setTeamTowerKills(toInt(row[i++]));
        dto.setTeamInhibitorKills(toInt(row[i++]));

        dto.setGoldTimeline(toIntegerArray(row[i++]));
        dto.setTimestamps(toIntegerArray(row[i]));

        return dto;
    }

    private int toInt(Object value) {
        return value != null ? ((Number) value).intValue() : 0;
    }

    private Integer[] toIntegerArray(Object value) {
        if (value == null) {
            return null;
        }
        return (Integer[]) value;
    }

}
