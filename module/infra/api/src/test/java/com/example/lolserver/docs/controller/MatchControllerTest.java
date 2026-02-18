package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.match.MatchController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.TeamInfoData;
import com.example.lolserver.domain.match.domain.gameData.timeline.ItemSeqData;
import com.example.lolserver.domain.match.domain.gameData.timeline.ParticipantTimeline;
import com.example.lolserver.domain.match.domain.gameData.timeline.SkillSeqData;
import com.example.lolserver.domain.match.domain.gameData.value.ItemValue;
import com.example.lolserver.domain.match.domain.gameData.value.StatValue;
import com.example.lolserver.domain.match.domain.gameData.value.Style;
import com.example.lolserver.domain.match.domain.TeamData;
import com.example.lolserver.domain.match.application.MatchService;
import com.example.lolserver.domain.match.application.dto.GameResponse;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.support.Page;



import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest extends RestDocsSupport {

    @Mock
    private MatchService matchService;

    @InjectMocks
    private MatchController matchController;

    @Override
    protected Object initController() {
        return matchController;
    }

    @DisplayName("매치 상세 정보 조회 API")
    @Test
    void fetchMatchResponse() throws Exception {
        // given
        String matchId = "KR_123456789";

        ParticipantData participant = mock(ParticipantData.class);
        given(participant.getChampionName()).willReturn("Ahri");
        given(participant.getKills()).willReturn(10);
        given(participant.getDeaths()).willReturn(2);
        given(participant.getAssists()).willReturn(5);
        given(participant.isWin()).willReturn(true);

        ItemSeqData itemSeqData = mock(ItemSeqData.class);
        given(itemSeqData.getItemId()).willReturn(3157);
        given(itemSeqData.getType()).willReturn("ITEM_PURCHASED");
        given(itemSeqData.getMinute()).willReturn(12L);

        SkillSeqData skillSeqData = mock(SkillSeqData.class);
        given(skillSeqData.getSkillSlot()).willReturn(1);
        given(skillSeqData.getType()).willReturn("SKILL_LEVEL_UP");
        given(skillSeqData.getMinute()).willReturn(1L);

        given(participant.getItemSeq()).willReturn(List.of(itemSeqData));
        given(participant.getSkillSeq()).willReturn(List.of(skillSeqData));

        GameInfoData gameInfoData = mock(GameInfoData.class);
        given(gameInfoData.getGameMode()).willReturn("CLASSIC");
        given(gameInfoData.getGameDuration()).willReturn(1800L);

        TeamInfoData team100 = mock(TeamInfoData.class);
        given(team100.getTeamId()).willReturn(100);
        given(team100.isWin()).willReturn(true);
        given(team100.getChampionKills()).willReturn(25);
        TeamInfoData team200 = mock(TeamInfoData.class);
        given(team200.getTeamId()).willReturn(200);
        given(team200.isWin()).willReturn(false);
        given(team200.getChampionKills()).willReturn(15);

        GameResponse gameData = mock(GameResponse.class);
        given(gameData.getGameInfoData()).willReturn(gameInfoData);
        given(gameData.getParticipantData()).willReturn(List.of(participant));
        given(gameData.getTeamInfoData()).willReturn(TeamData.builder().blueTeam(team100).redTeam(team200).build());

        given(matchService.getGameData(anyString())).willReturn(gameData);

        // when & then
        mockMvc.perform(
                        get("/api/v1/matches/{matchId}", matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("match-get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("matchId").description("조회할 매치 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data.gameInfoData").type(JsonFieldType.OBJECT).description("게임 정보"),
                                subsectionWithPath("data.participantData[]").type(JsonFieldType.ARRAY).description("전체 참가자 목록"),
                                subsectionWithPath("data.teamInfoData").type(JsonFieldType.OBJECT).description("팀 정보 (blueTeam, redTeam)")
                        )
                ));
    }

    @DisplayName("매치 ID 목록 조회 API")
    @Test
    void findAllMatchIds() throws Exception {
        // given
        MatchCommand request = MatchCommand.builder().puuid("puuid-1234").queueId(420).pageNo(1).region("kr").build();
        List<String> matchIds = List.of("KR_123456789", "KR_987654321");

        Page<String> stringPage = new Page<>(matchIds, true);
        given(matchService.findAllMatchIds(any(MatchCommand.class))).willReturn(stringPage);

        // when & then
        mockMvc.perform(
                        get("/api/v1/matches/matchIds")
                                .param("puuid", request.getPuuid())
                                .param("queueId", String.valueOf(request.getQueueId()))
                                .param("pageNo", String.valueOf(request.getPageNo()))
                                .param("region", request.getRegion())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("match-get-ids",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("puuid").description("조회할 유저의 PUUID"),
                                parameterWithName("queueId").description("큐 ID (e.g., 420:솔로랭크, 430:일반, 450:칼바람)").optional(),
                                parameterWithName("pageNo").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("region").description("지역")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("매치 ID 목록"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 항목 존재 여부")
                        )
                ));
    }

    @DisplayName("매치 목록 조회 API")
    @Test
    void fetchGameData() throws Exception {
        // given
        MatchCommand request = MatchCommand.builder().puuid("puuid-1234").queueId(420).pageNo(1).region("kr").build();
        
        GameResponse gameData = mock(GameResponse.class);

        Page<GameResponse> pageOfGameData = new Page<>(List.of(gameData), false);

        given(matchService.getMatches(any(MatchCommand.class))).willReturn(pageOfGameData);

        // when & then
        mockMvc.perform(
                        get("/api/v1/matches")
                                .param("puuid", request.getPuuid())
                                .param("queueId", String.valueOf(request.getQueueId()))
                                .param("pageNo", String.valueOf(request.getPageNo()))
                                .param("region", request.getRegion())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("match-get-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("puuid").description("조회할 유저의 PUUID"),
                                parameterWithName("queueId").description("큐 ID (e.g., 420:솔로랭크, 430:일반, 450:칼바람)").optional(),
                                parameterWithName("pageNo").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("region").description("지역")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data.content[]").type(JsonFieldType.ARRAY).description("매치 데이터 목록 (GameResponse)"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @DisplayName("소환사별 매치 목록 배치 조회 API")
    @Test
    void fetchMatchesBySummoner() throws Exception {
        // given
        String puuid = "puuid-1234";

        GameInfoData gameInfoData = new GameInfoData(
                "2", 1700000000000L, 1800L, 1700001800000L,
                "CLASSIC", 1700000000000L, "MATCHED_GAME",
                "14.1.1", 11, "KR", 420, "", "KR_7123456789", 1600
        );

        ItemValue itemValue = ItemValue.builder()
                .item0(3157).item1(3020).item2(3089).item3(3135)
                .item4(3116).item5(3165).item6(3340)
                .build();

        StatValue statValue = StatValue.builder()
                .defense(5002).flex(5008).offense(5005)
                .build();

        Style style = new Style(
                8100, new int[]{8112, 8126, 8138, 8135},
                8300, new int[]{8304, 8347}
        );

        ItemSeqData itemSeqData = ItemSeqData.builder()
                .itemId(3157).minute(12L).type("ITEM_PURCHASED")
                .build();

        SkillSeqData skillSeqData = SkillSeqData.builder()
                .skillSlot(1).minute(1L).type("SKILL_LEVEL_UP")
                .build();

        ParticipantData participant = ParticipantData.builder()
                .profileIcon(4892)
                .riotIdGameName("TestSummoner").riotIdTagline("KR1")
                .puuid("test-puuid-1234").summonerLevel(350).summonerId("summoner-id-1234")
                .tier("DIAMOND").tierRank("I").absolutePoints(2850)
                .individualPosition("MIDDLE")
                .kills(10).deaths(2).assists(5)
                .champExperience(15000).champLevel(18)
                .championId(103).championName("Ahri")
                .consumablesPurchased(2).goldEarned(14500)
                .item(itemValue)
                .summoner1Id(4).summoner2Id(14)
                .itemsPurchased(15).participantId(1)
                .statValue(statValue).style(style)
                .visionScore(32).totalMinionsKilled(185).neutralMinionsKilled(20)
                .totalDamageDealtToChampions(28000).totalDamageTaken(15000)
                .visionWardsBoughtInGame(3).wardsKilled(5).wardsPlaced(12)
                .doubleKills(2).tripleKills(1).quadraKills(0).pentaKills(0)
                .kda(7.5).teamDamagePercentage(28.5).goldPerMinute(480.0).killParticipation(62.5)
                .teamId(100).teamPosition("MIDDLE").win(true)
                .timePlayed(1800).timeCCingOthers(25)
                .lane("MIDDLE").role("SOLO")
                .placement(0)
                .playerAugment1(0).playerAugment2(0).playerAugment3(0).playerAugment4(0)
                .itemSeq(List.of(itemSeqData))
                .skillSeq(List.of(skillSeqData))
                .build();

        TeamInfoData blueTeam = new TeamInfoData(
                100, true, 35, 2, 3, 9, 2,
                List.of(103, 236, 412, 110, 22),
                List.of(1, 2, 3, 4, 5)
        );

        TeamInfoData redTeam = new TeamInfoData(
                200, false, 20, 1, 1, 3, 0,
                List.of(238, 64, 127, 21, 89),
                List.of(6, 7, 8, 9, 10)
        );

        GameResponse gameData = new GameResponse();
        gameData.setGameInfoData(gameInfoData);
        gameData.setParticipantData(List.of(participant));
        gameData.setTeamInfoData(TeamData.builder().blueTeam(blueTeam).redTeam(redTeam).build());

        Page<GameResponse> pageOfGameData = new Page<>(List.of(gameData), false);

        given(matchService.getMatchesBatch(any(MatchCommand.class))).willReturn(pageOfGameData);

        // when & then
        mockMvc.perform(
                        get("/api/v1/summoners/{puuid}/matches", puuid)
                                .param("queueId", "420")
                                .param("pageNo", "1")
                                .param("region", "kr")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("match-summoner-get-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("puuid").description("조회할 소환사의 PUUID")
                        ),
                        queryParameters(
                                parameterWithName("queueId").description("큐 ID (e.g., 420:솔로랭크, 430:일반, 450:칼바람)").optional(),
                                parameterWithName("pageNo").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("region").description("지역").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),

                                // GameInfoData
                                fieldWithPath("data.content[].gameInfoData.dataVersion").type(JsonFieldType.STRING).description("데이터 버전"),
                                fieldWithPath("data.content[].gameInfoData.gameCreation").type(JsonFieldType.NUMBER).description("게임 생성 타임스탬프 (epoch ms)"),
                                fieldWithPath("data.content[].gameInfoData.gameDuration").type(JsonFieldType.NUMBER).description("게임 지속 시간 (초)"),
                                fieldWithPath("data.content[].gameInfoData.gameEndTimestamp").type(JsonFieldType.NUMBER).description("게임 종료 타임스탬프 (epoch ms)"),
                                fieldWithPath("data.content[].gameInfoData.gameMode").type(JsonFieldType.STRING).description("게임 모드 (CLASSIC, ARAM 등)"),
                                fieldWithPath("data.content[].gameInfoData.gameStartTimestamp").type(JsonFieldType.NUMBER).description("게임 시작 타임스탬프 (epoch ms)"),
                                fieldWithPath("data.content[].gameInfoData.gameType").type(JsonFieldType.STRING).description("게임 타입"),
                                fieldWithPath("data.content[].gameInfoData.gameVersion").type(JsonFieldType.STRING).description("게임 버전"),
                                fieldWithPath("data.content[].gameInfoData.mapId").type(JsonFieldType.NUMBER).description("맵 ID (11: 소환사의 협곡, 12: 칼바람 나락)"),
                                fieldWithPath("data.content[].gameInfoData.platformId").type(JsonFieldType.STRING).description("플랫폼 ID (KR, NA1 등)"),
                                fieldWithPath("data.content[].gameInfoData.queueId").type(JsonFieldType.NUMBER).description("큐 ID (420: 솔로랭크, 430: 일반 등)"),
                                fieldWithPath("data.content[].gameInfoData.tournamentCode").type(JsonFieldType.STRING).description("토너먼트 코드"),
                                fieldWithPath("data.content[].gameInfoData.matchId").type(JsonFieldType.STRING).description("매치 ID"),
                                fieldWithPath("data.content[].gameInfoData.averageTier").type(JsonFieldType.NUMBER).description("평균 티어 (숫자)"),

                                // ParticipantData - 유저 정보
                                fieldWithPath("data.content[].participantData[].profileIcon").type(JsonFieldType.NUMBER).description("프로필 아이콘 ID"),
                                fieldWithPath("data.content[].participantData[].riotIdGameName").type(JsonFieldType.STRING).description("Riot ID 게임 이름"),
                                fieldWithPath("data.content[].participantData[].riotIdTagline").type(JsonFieldType.STRING).description("Riot ID 태그라인"),
                                fieldWithPath("data.content[].participantData[].puuid").type(JsonFieldType.STRING).description("소환사 PUUID"),
                                fieldWithPath("data.content[].participantData[].summonerLevel").type(JsonFieldType.NUMBER).description("소환사 레벨"),
                                fieldWithPath("data.content[].participantData[].summonerId").type(JsonFieldType.STRING).description("소환사 ID"),
                                fieldWithPath("data.content[].participantData[].tier").type(JsonFieldType.STRING).description("소환사 티어").optional(),
                                fieldWithPath("data.content[].participantData[].tierRank").type(JsonFieldType.STRING).description("소환사 티어 등급 (I~IV)").optional(),
                                fieldWithPath("data.content[].participantData[].absolutePoints").type(JsonFieldType.NUMBER).description("절대 포인트 (티어+등급+LP 수치화)").optional(),

                                // ParticipantData - 게임 정보
                                fieldWithPath("data.content[].participantData[].individualPosition").type(JsonFieldType.STRING).description("개인 포지션 (TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY)"),
                                fieldWithPath("data.content[].participantData[].kills").type(JsonFieldType.NUMBER).description("킬 수"),
                                fieldWithPath("data.content[].participantData[].deaths").type(JsonFieldType.NUMBER).description("데스 수"),
                                fieldWithPath("data.content[].participantData[].assists").type(JsonFieldType.NUMBER).description("어시스트 수"),
                                fieldWithPath("data.content[].participantData[].champExperience").type(JsonFieldType.NUMBER).description("챔피언 경험치"),
                                fieldWithPath("data.content[].participantData[].champLevel").type(JsonFieldType.NUMBER).description("챔피언 레벨"),
                                fieldWithPath("data.content[].participantData[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.content[].participantData[].championName").type(JsonFieldType.STRING).description("챔피언 이름"),
                                fieldWithPath("data.content[].participantData[].consumablesPurchased").type(JsonFieldType.NUMBER).description("소모품 구매 횟수"),
                                fieldWithPath("data.content[].participantData[].goldEarned").type(JsonFieldType.NUMBER).description("획득 골드"),
                                fieldWithPath("data.content[].participantData[].summoner1Id").type(JsonFieldType.NUMBER).description("소환사 주문 1 ID"),
                                fieldWithPath("data.content[].participantData[].summoner2Id").type(JsonFieldType.NUMBER).description("소환사 주문 2 ID"),
                                fieldWithPath("data.content[].participantData[].itemsPurchased").type(JsonFieldType.NUMBER).description("아이템 구매 횟수"),
                                fieldWithPath("data.content[].participantData[].participantId").type(JsonFieldType.NUMBER).description("참가자 ID"),
                                fieldWithPath("data.content[].participantData[].visionScore").type(JsonFieldType.NUMBER).description("시야 점수"),
                                fieldWithPath("data.content[].participantData[].totalMinionsKilled").type(JsonFieldType.NUMBER).description("총 미니언 처치 수"),
                                fieldWithPath("data.content[].participantData[].neutralMinionsKilled").type(JsonFieldType.NUMBER).description("중립 몬스터 처치 수"),
                                fieldWithPath("data.content[].participantData[].totalDamageDealtToChampions").type(JsonFieldType.NUMBER).description("챔피언에게 가한 총 피해량"),
                                fieldWithPath("data.content[].participantData[].totalDamageTaken").type(JsonFieldType.NUMBER).description("받은 총 피해량"),
                                fieldWithPath("data.content[].participantData[].visionWardsBoughtInGame").type(JsonFieldType.NUMBER).description("제어 와드 구매 횟수"),
                                fieldWithPath("data.content[].participantData[].wardsKilled").type(JsonFieldType.NUMBER).description("와드 제거 횟수"),
                                fieldWithPath("data.content[].participantData[].wardsPlaced").type(JsonFieldType.NUMBER).description("와드 설치 횟수"),
                                fieldWithPath("data.content[].participantData[].doubleKills").type(JsonFieldType.NUMBER).description("더블킬 횟수"),
                                fieldWithPath("data.content[].participantData[].tripleKills").type(JsonFieldType.NUMBER).description("트리플킬 횟수"),
                                fieldWithPath("data.content[].participantData[].quadraKills").type(JsonFieldType.NUMBER).description("쿼드라킬 횟수"),
                                fieldWithPath("data.content[].participantData[].pentaKills").type(JsonFieldType.NUMBER).description("펜타킬 횟수"),
                                fieldWithPath("data.content[].participantData[].kda").type(JsonFieldType.NUMBER).description("KDA"),
                                fieldWithPath("data.content[].participantData[].teamDamagePercentage").type(JsonFieldType.NUMBER).description("팀 피해 분담률 (%)"),
                                fieldWithPath("data.content[].participantData[].goldPerMinute").type(JsonFieldType.NUMBER).description("분당 골드"),
                                fieldWithPath("data.content[].participantData[].killParticipation").type(JsonFieldType.NUMBER).description("킬 관여율 (%)"),

                                // ParticipantData - 아이템
                                fieldWithPath("data.content[].participantData[].item.item0").type(JsonFieldType.NUMBER).description("아이템 슬롯 0"),
                                fieldWithPath("data.content[].participantData[].item.item1").type(JsonFieldType.NUMBER).description("아이템 슬롯 1"),
                                fieldWithPath("data.content[].participantData[].item.item2").type(JsonFieldType.NUMBER).description("아이템 슬롯 2"),
                                fieldWithPath("data.content[].participantData[].item.item3").type(JsonFieldType.NUMBER).description("아이템 슬롯 3"),
                                fieldWithPath("data.content[].participantData[].item.item4").type(JsonFieldType.NUMBER).description("아이템 슬롯 4"),
                                fieldWithPath("data.content[].participantData[].item.item5").type(JsonFieldType.NUMBER).description("아이템 슬롯 5"),
                                fieldWithPath("data.content[].participantData[].item.item6").type(JsonFieldType.NUMBER).description("아이템 슬롯 6 (장신구)"),

                                // ParticipantData - 스탯
                                fieldWithPath("data.content[].participantData[].statValue.defense").type(JsonFieldType.NUMBER).description("룬 방어 스탯"),
                                fieldWithPath("data.content[].participantData[].statValue.flex").type(JsonFieldType.NUMBER).description("룬 유연 스탯"),
                                fieldWithPath("data.content[].participantData[].statValue.offense").type(JsonFieldType.NUMBER).description("룬 공격 스탯"),

                                // ParticipantData - 룬 스타일
                                fieldWithPath("data.content[].participantData[].style.primaryRuneId").type(JsonFieldType.NUMBER).description("주 룬 트리 ID"),
                                fieldWithPath("data.content[].participantData[].style.primaryRuneIds").type(JsonFieldType.ARRAY).description("주 룬 ID 목록"),
                                fieldWithPath("data.content[].participantData[].style.secondaryRuneId").type(JsonFieldType.NUMBER).description("부 룬 트리 ID"),
                                fieldWithPath("data.content[].participantData[].style.secondaryRuneIds").type(JsonFieldType.ARRAY).description("부 룬 ID 목록"),

                                // ParticipantData - 팀 정보
                                fieldWithPath("data.content[].participantData[].teamId").type(JsonFieldType.NUMBER).description("팀 ID (100: 블루, 200: 레드)"),
                                fieldWithPath("data.content[].participantData[].teamPosition").type(JsonFieldType.STRING).description("팀 내 포지션"),
                                fieldWithPath("data.content[].participantData[].win").type(JsonFieldType.BOOLEAN).description("승리 여부"),
                                fieldWithPath("data.content[].participantData[].timePlayed").type(JsonFieldType.NUMBER).description("플레이 시간 (초)"),
                                fieldWithPath("data.content[].participantData[].timeCCingOthers").type(JsonFieldType.NUMBER).description("CC기 적중 시간"),
                                fieldWithPath("data.content[].participantData[].lane").type(JsonFieldType.STRING).description("라인"),
                                fieldWithPath("data.content[].participantData[].role").type(JsonFieldType.STRING).description("역할"),

                                // ParticipantData - 아레나
                                fieldWithPath("data.content[].participantData[].placement").type(JsonFieldType.NUMBER).description("아레나 배치 순위 (일반 게임: 0)"),
                                fieldWithPath("data.content[].participantData[].playerAugment1").type(JsonFieldType.NUMBER).description("아레나 증강 1 (일반 게임: 0)"),
                                fieldWithPath("data.content[].participantData[].playerAugment2").type(JsonFieldType.NUMBER).description("아레나 증강 2 (일반 게임: 0)"),
                                fieldWithPath("data.content[].participantData[].playerAugment3").type(JsonFieldType.NUMBER).description("아레나 증강 3 (일반 게임: 0)"),
                                fieldWithPath("data.content[].participantData[].playerAugment4").type(JsonFieldType.NUMBER).description("아레나 증강 4 (일반 게임: 0)"),

                                // ParticipantData - 타임라인
                                fieldWithPath("data.content[].participantData[].itemSeq[].itemId").type(JsonFieldType.NUMBER).description("아이템 ID"),
                                fieldWithPath("data.content[].participantData[].itemSeq[].minute").type(JsonFieldType.NUMBER).description("구매 시간 (분)"),
                                fieldWithPath("data.content[].participantData[].itemSeq[].type").type(JsonFieldType.STRING).description("이벤트 타입 (ITEM_PURCHASED 등)"),
                                fieldWithPath("data.content[].participantData[].skillSeq[].skillSlot").type(JsonFieldType.NUMBER).description("스킬 슬롯 (1-4)"),
                                fieldWithPath("data.content[].participantData[].skillSeq[].minute").type(JsonFieldType.NUMBER).description("레벨업 시간 (분)"),
                                fieldWithPath("data.content[].participantData[].skillSeq[].type").type(JsonFieldType.STRING).description("이벤트 타입 (SKILL_LEVEL_UP 등)"),

                                // TeamInfoData - 블루팀
                                fieldWithPath("data.content[].teamInfoData.blueTeam.teamId").type(JsonFieldType.NUMBER).description("블루팀 ID (100)"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.win").type(JsonFieldType.BOOLEAN).description("블루팀 승리 여부"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.championKills").type(JsonFieldType.NUMBER).description("블루팀 총 킬 수"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.baronKills").type(JsonFieldType.NUMBER).description("블루팀 바론 처치 수"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.dragonKills").type(JsonFieldType.NUMBER).description("블루팀 드래곤 처치 수"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.towerKills").type(JsonFieldType.NUMBER).description("블루팀 타워 파괴 수"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.inhibitorKills").type(JsonFieldType.NUMBER).description("블루팀 억제기 파괴 수"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.championId").type(JsonFieldType.ARRAY).description("블루팀 챔피언 ID 목록"),
                                fieldWithPath("data.content[].teamInfoData.blueTeam.pickTurn").type(JsonFieldType.ARRAY).description("블루팀 픽 순서"),

                                // TeamInfoData - 레드팀
                                fieldWithPath("data.content[].teamInfoData.redTeam.teamId").type(JsonFieldType.NUMBER).description("레드팀 ID (200)"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.win").type(JsonFieldType.BOOLEAN).description("레드팀 승리 여부"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.championKills").type(JsonFieldType.NUMBER).description("레드팀 총 킬 수"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.baronKills").type(JsonFieldType.NUMBER).description("레드팀 바론 처치 수"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.dragonKills").type(JsonFieldType.NUMBER).description("레드팀 드래곤 처치 수"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.towerKills").type(JsonFieldType.NUMBER).description("레드팀 타워 파괴 수"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.inhibitorKills").type(JsonFieldType.NUMBER).description("레드팀 억제기 파괴 수"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.championId").type(JsonFieldType.ARRAY).description("레드팀 챔피언 ID 목록"),
                                fieldWithPath("data.content[].teamInfoData.redTeam.pickTurn").type(JsonFieldType.ARRAY).description("레드팀 픽 순서")
                        )
                ));
    }

    @DisplayName("랭크 챔피언 통계 조회 API")
    @Test
    void getRankChampions() throws Exception {
        // given
        MSChampionCommand request = new MSChampionCommand();
        request.setPuuid("puuid-1234");
        request.setSeason(2024);

        MSChampion champion = mock(MSChampion.class);
        given(champion.getChampionId()).willReturn(266);
        given(champion.getChampionName()).willReturn("Aatrox");
        given(champion.getKills()).willReturn(10.5);
        given(champion.getDeaths()).willReturn(5.5);
        given(champion.getAssists()).willReturn(8.5);
        given(champion.getWin()).willReturn(20L);
        given(champion.getLosses()).willReturn(10L);
        given(champion.getWinRate()).willReturn(66.7);
        given(champion.getDamagePerMinute()).willReturn(850.0);
        given(champion.getKda()).willReturn(3.45);
        given(champion.getLaneMinionsFirst10Minutes()).willReturn(72.5);
        given(champion.getDamageTakenOnTeamPercentage()).willReturn(22.5);
        given(champion.getGoldPerMinute()).willReturn(420.0);
        given(champion.getPlayCount()).willReturn(30L);

        given(matchService.getRankChampions(any(MSChampionCommand.class))).willReturn(List.of(champion));

        // when & then
        mockMvc.perform(
                        get("/api/v1/rank/champions")
                                .param("puuid", request.getPuuid())
                                .param("season", String.valueOf(request.getSeason()))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("match-get-rank-champions",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("puuid").description("조회할 유저의 PUUID"),
                                parameterWithName("season").description("시즌").optional(),
                                parameterWithName("queueId").description("큐 ID (e.g., 420:솔로랭크, 440:자유랭크)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data[].championName").type(JsonFieldType.STRING).description("챔피언 이름"),
                                fieldWithPath("data[].kills").type(JsonFieldType.NUMBER).description("평균 킬"),
                                fieldWithPath("data[].deaths").type(JsonFieldType.NUMBER).description("평균 데스"),
                                fieldWithPath("data[].assists").type(JsonFieldType.NUMBER).description("평균 어시스트"),
                                fieldWithPath("data[].win").type(JsonFieldType.NUMBER).description("승리 횟수"),
                                fieldWithPath("data[].losses").type(JsonFieldType.NUMBER).description("패배 횟수"),
                                fieldWithPath("data[].winRate").type(JsonFieldType.NUMBER).description("승률 (%)"),
                                fieldWithPath("data[].damagePerMinute").type(JsonFieldType.NUMBER).description("분당 피해량"),
                                fieldWithPath("data[].kda").type(JsonFieldType.NUMBER).description("KDA"),
                                fieldWithPath("data[].laneMinionsFirst10Minutes").type(JsonFieldType.NUMBER).description("10분 CS"),
                                fieldWithPath("data[].damageTakenOnTeamPercentage").type(JsonFieldType.NUMBER).description("팀 피해 분담률 (%)"),
                                fieldWithPath("data[].goldPerMinute").type(JsonFieldType.NUMBER).description("분당 골드"),
                                fieldWithPath("data[].playCount").type(JsonFieldType.NUMBER).description("플레이 횟수")
                        )
                ));
    }

    @DisplayName("매치 타임라인 조회 API")
    @Test
    void getTimeline() throws Exception {
        // given
        String matchId = "KR_123456789";

        ItemSeqData itemSeq = mock(ItemSeqData.class);
        given(itemSeq.getItemId()).willReturn(1001);
        given(itemSeq.getType()).willReturn("ITEM_PURCHASED");
        given(itemSeq.getMinute()).willReturn(1L);

        SkillSeqData skillSeq = mock(SkillSeqData.class);
        given(skillSeq.getSkillSlot()).willReturn(1);
        given(skillSeq.getType()).willReturn("SKILL_LEVEL_UP");
        given(skillSeq.getMinute()).willReturn(0L);

        ParticipantTimeline participantTimeline = mock(ParticipantTimeline.class);
        given(participantTimeline.getItemSeq()).willReturn(List.of(itemSeq));
        given(participantTimeline.getSkillSeq()).willReturn(List.of(skillSeq));

        Map<Integer, ParticipantTimeline> participants = new HashMap<>();
        participants.put(1, participantTimeline);

        TimelineData timelineData = mock(TimelineData.class);
        given(timelineData.getParticipants()).willReturn(participants);

        given(matchService.getTimelineData(anyString())).willReturn(timelineData);

        // when & then
        mockMvc.perform(
                        get("/api/v1/match/timeline/{matchId}", matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("match-get-timeline",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("matchId").description("조회할 매치 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.participants").type(JsonFieldType.OBJECT).description("참가자별 타임라인 데이터 (key: participantId)"),
                                fieldWithPath("data.participants.*.itemSeq[]").type(JsonFieldType.ARRAY).description("아이템 구매 순서"),
                                fieldWithPath("data.participants.*.itemSeq[].itemId").type(JsonFieldType.NUMBER).description("아이템 ID"),
                                fieldWithPath("data.participants.*.itemSeq[].minute").type(JsonFieldType.NUMBER).description("구매 시간 (분)"),
                                fieldWithPath("data.participants.*.itemSeq[].type").type(JsonFieldType.STRING).description("이벤트 타입"),
                                fieldWithPath("data.participants.*.skillSeq[]").type(JsonFieldType.ARRAY).description("스킬 레벨업 순서"),
                                fieldWithPath("data.participants.*.skillSeq[].skillSlot").type(JsonFieldType.NUMBER).description("스킬 슬롯 (1-4)"),
                                fieldWithPath("data.participants.*.skillSeq[].minute").type(JsonFieldType.NUMBER).description("레벨업 시간 (분)"),
                                fieldWithPath("data.participants.*.skillSeq[].type").type(JsonFieldType.STRING).description("이벤트 타입")
                        )
                ));
    }
}
