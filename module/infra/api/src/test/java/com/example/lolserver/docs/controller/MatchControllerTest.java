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
        given(participant.getSummonerName()).willReturn("MySummoner");
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

        GameResponse gameData = mock(GameResponse.class);

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
                                subsectionWithPath("data.content[]").type(JsonFieldType.ARRAY).description("매치 데이터 목록 (GameResponse)"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
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
