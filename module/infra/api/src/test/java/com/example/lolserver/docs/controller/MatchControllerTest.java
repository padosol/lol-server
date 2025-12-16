package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.match.MatchController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.match.command.MSChampionCommand;
import com.example.lolserver.domain.match.command.MatchCommand;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.SeqTypeData;
import com.example.lolserver.domain.match.domain.gameData.TeamInfoData;
import com.example.lolserver.domain.match.service.MatchService;
import com.example.lolserver.domain.match.domain.GameData;
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

        ParticipantData myData = mock(ParticipantData.class);
        given(myData.getSummonerName()).willReturn("MySummoner");
        given(myData.getChampionName()).willReturn("Ahri");
        given(myData.getKills()).willReturn(10);
        given(myData.getDeaths()).willReturn(2);
        given(myData.getAssists()).willReturn(5);
        given(myData.isWin()).willReturn(true);

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

        GameData gameData = mock(GameData.class);
        given(gameData.getMyData()).willReturn(myData);
        given(gameData.getGameInfoData()).willReturn(gameInfoData);
        given(gameData.getParticipantData()).willReturn(List.of(myData));
        given(gameData.getTeamInfoData()).willReturn(Map.of(100, team100, 200, team200));

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
                        relaxedResponseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.myData.summonerName").type(JsonFieldType.STRING).description("내 소환사 이름"),
                                fieldWithPath("data.gameInfoData.gameMode").type(JsonFieldType.STRING).description("게임 모드")
                        ),
                        pathParameters(
                                parameterWithName("matchId").description("조회할 매치 ID")
                        )
                ));
    }

    @DisplayName("매치 ID 목록 조회 API")
    @Test
    void findAllMatchIds() throws Exception {
        // given
        MatchCommand request = MatchCommand.builder().puuid("puuid-1234").queueId(420).pageNo(1).region("kr").build();
        List<String> matchIds = List.of("KR_123456789", "KR_987654321");
        given(matchService.findAllMatchIds(any(MatchCommand.class))).willReturn(matchIds);

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
                        relaxedResponseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("매치 ID 목록"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)")
                        )
                ));
    }

    @DisplayName("매치 목록 조회 API")
    @Test
    void fetchGameData() throws Exception {
        // given
        MatchCommand request = MatchCommand.builder().puuid("puuid-1234").queueId(420).pageNo(1).region("kr").build();
        
        // Mock GameData for the Page object
        ParticipantData myData = mock(ParticipantData.class);
        given(myData.getSummonerName()).willReturn("MySummoner");
        given(myData.getChampionName()).willReturn("Ahri");
        given(myData.getKills()).willReturn(10);
        given(myData.getDeaths()).willReturn(2);
        given(myData.getAssists()).willReturn(5);
        given(myData.isWin()).willReturn(true);

        GameData gameData = mock(GameData.class);
        given(gameData.getMyData()).willReturn(myData);
        // Assuming other fields of gameData are not critical for this test or will be mocked as needed

        // Construct a real Page object
        Page<GameData> pageOfGameData = new Page<>(List.of(gameData), false); // Assuming hasNext is false for a single page

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
                        relaxedResponseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("data.content[].myData.summonerName").type(JsonFieldType.STRING).description("내 소환사 이름"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)")
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
        given(champion.getWinRate()).willReturn(7.5);
        given(champion.getKda()).willReturn(3.2);
        given(champion.getDamageTakenOnTeamPercentage()).willReturn(8.1);
        given(champion.getChampionId()).willReturn(266);
        given(champion.getChampionName()).willReturn("Aatrox");
        given(champion.getWin()).willReturn(100L);
        given(champion.getPlayCount()).willReturn(30L);
        given(champion.getKills()).willReturn(10.5);
        given(champion.getDeaths()).willReturn(15.5);
        given(champion.getAssists()).willReturn(15.5);
        given(champion.getLaneMinionsFirst10Minutes()).willReturn(15.5);
        given(champion.getGoldPerMinute()).willReturn(15.5);
        given(champion.getDamagePerMinute()).willReturn(20.0);

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
                                parameterWithName("queueId").description("큐 ID").optional(),
                                parameterWithName("platform").description("플랫폼(지역)").optional()
                        ),
                        relaxedResponseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("data[].championName").type(JsonFieldType.STRING).description("챔피언 이름"),
                                fieldWithPath("data[].win").type(JsonFieldType.NUMBER).description("승리 횟수"),
                                fieldWithPath("data[].playCount").type(JsonFieldType.NUMBER).description("플레이 횟수"),
                                fieldWithPath("data[].kills").type(JsonFieldType.NUMBER).description("평균 Kills"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)")
                        )
                ));
    }

    @DisplayName("매치 타임라인 조회 API")
    @Test
    void getTimeline() throws Exception {
        // given
        String matchId = "KR_123456789";

        SeqTypeData itemSeq = mock(SeqTypeData.class);
        given(itemSeq.getId()).willReturn(1001);
        given(itemSeq.getType()).willReturn("ITEM_PURCHASED");
        given(itemSeq.getMinute()).willReturn(1L);

        Map<String, List<SeqTypeData>> participantData = new HashMap<>();
        participantData.put("ITEM_SEQ", List.of(itemSeq));
        
        Map<Integer, Map<String, List<SeqTypeData>>> data = new HashMap<>();
        data.put(1, participantData);

        TimelineData timelineData = mock(TimelineData.class);
        given(timelineData.getData()).willReturn(data);

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
                        relaxedResponseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("data.data.1.ITEM_SEQ[].id").type(JsonFieldType.NUMBER).description("아이템 ID"),
                                fieldWithPath("data.data.1.ITEM_SEQ[].type").type(JsonFieldType.STRING).description("이벤트 타입"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)")
                        )
                ));
    }
}
