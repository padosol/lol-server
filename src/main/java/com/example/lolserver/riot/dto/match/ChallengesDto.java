package com.example.lolserver.riot.dto.match;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChallengesDto {

    @JsonProperty("12AssistStreakCount")
    private int assistStreakCount12;

    @JsonProperty("InfernalScalePickup")
    private int infernalScalePickup;
    private int abilityUses;
    private int acesBefore15Minutes;
    private int alliedJungleMonsterKills;
    private int baronTakedowns;
    private int blastConeOppositeOpponentCount;
    private int bountyGold;
    private int buffsStolen;
    private int completeSupportQuestInTime;
    private double controlWardTimeCoverageInRiverOrEnemyHalf;
    private int controlWardsPlaced;
    private double damagePerMinute;
    private double damageTakenOnTeamPercentage;
    private int dancedWithRiftHerald;
    private int deathsByEnemyChamps;
    private int dodgeSkillShotsSmallWindow;
    private int doubleAces;
    private int dragonTakedowns;
    private int earlyLaningPhaseGoldExpAdvantage;
    private int effectiveHealAndShielding;
    private int elderDragonKillsWithOpposingSoul;
    private int elderDragonMultikills;
    private int enemyChampionImmobilizations;
    private int enemyJungleMonsterKills;
    private int epicMonsterKillsNearEnemyJungler;
    private int epicMonsterKillsWithin30SecondsOfSpawn;
    private int epicMonsterSteals;
    private int epicMonsterStolenWithoutSmite;
    private int firstTurretKilled;
    private double firstTurretKilledTime;
    private int fistBumpParticipation;
    private int flawlessAces;
    private int fullTeamTakedown;
    private double gameLength;
    private int getTakedownsInAllLanesEarlyJungleAsLaner;
    private double goldPerMinute;
    private int hadOpenNexus;
    private int immobilizeAndKillWithAlly;
    private int initialBuffCount;
    private int initialCrabCount;
    private int jungleCsBefore10Minutes;
    private int junglerTakedownsNearDamagedEpicMonster;
    private int kTurretsDestroyedBeforePlatesFall;
    private double kda;
    private int killAfterHiddenWithAlly;
    private double killParticipation;
    private int killedChampTookFullTeamDamageSurvived;
    private int killingSprees;
    private int killsNearEnemyTurret;
    private int killsOnOtherLanesEarlyJungleAsLaner;
    private int killsOnRecentlyHealedByAramPack;
    private int killsUnderOwnTurret;
    private int killsWithHelpFromEpicMonster;
    private int knockEnemyIntoTeamAndKill;
    private int landSkillShotsEarlyGame;
    private int laneMinionsFirst10Minutes;
    private int laningPhaseGoldExpAdvantage;
    private int legendaryCount;
    private List<Integer> legendaryItemUsed;
    private int lostAnInhibitor;
    private int maxCsAdvantageOnLaneOpponent;
    private int maxKillDeficit;
    private int maxLevelLeadLaneOpponent;
    private int mejaisFullStackInTime;
    private int moreEnemyJungleThanOpponent;
    private int multiKillOneSpell;
    private int multiTurretRiftHeraldCount;
    private int multikills;
    private int multikillsAfterAggressiveFlash;
    private int outerTurretExecutesBefore10Minutes;
    private int outnumberedKills;
    private int outnumberedNexusKill;
    private int perfectDragonSoulsTaken;
    private int perfectGame;
    private int pickKillWithAlly;
    private int playedChampSelectPosition;
    private int poroExplosions;
    private int quickCleanse;
    private int quickFirstTurret;
    private int quickSoloKills;
    private int riftHeraldTakedowns;
    private int saveAllyFromDeath;
    private int scuttleCrabKills;
    private int skillshotsDodged;
    private int skillshotsHit;
    private int snowballsHit;
    private int soloBaronKills;
    private int soloKills;
    private int stealthWardsPlaced;
    private int survivedSingleDigitHpCount;
    private int survivedThreeImmobilizesInFight;
    private int takedownOnFirstTurret;
    private int takedowns;
    private int takedownsAfterGainingLevelAdvantage;
    private int takedownsBeforeJungleMinionSpawn;
    private int takedownsFirstXMinutes;
    private int takedownsInAlcove;
    private int takedownsInEnemyFountain;
    private int teamBaronKills;
    private double teamDamagePercentage;
    private int teamElderDragonKills;
    private int teamRiftHeraldKills;
    private int tookLargeDamageSurvived;
    private int turretPlatesTaken;
    private int turretTakedowns;
    private int turretsTakenWithRiftHerald;
    private int twentyMinionsIn3SecondsCount;
    private int twoWardsOneSweeperCount;
    private int unseenRecalls;
    private double visionScoreAdvantageLaneOpponent;
    private double visionScorePerMinute;
    private int voidMonsterKill;
    private int wardTakedowns;
    private int wardTakedownsBefore20M;
    private int wardsGuarded;

}
