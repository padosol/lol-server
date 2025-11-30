package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.match.MatchController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.match.dto.MSChampionRequest;
import com.example.lolserver.domain.match.dto.MatchRequest;
import com.example.lolserver.domain.match.service.MatchService;
import com.example.lolserver.storage.db.core.repository.dto.data.GameData;
import com.example.lolserver.storage.db.core.repository.dto.data.TimelineData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.GameInfoData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.ParticipantData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.SeqTypeData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.TeamInfoData;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionDTO;
import com.example.lolserver.storage.db.core.repository.match.dto.MatchResponse;
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

        GameData gameData = new GameData();
        ParticipantData myData = ParticipantData.builder().summonerName("MySummoner").championName("Ahri").kills(10).deaths(2).assists(5).win(true).build();
        GameInfoData gameInfoData = new GameInfoData();
        gameInfoData.setGameMode("CLASSIC");
        gameInfoData.setGameDuration(1800);
        TeamInfoData team100 = new TeamInfoData();
        team100.setTeamId(100);
        team100.setWin(true);
        team100.setChampionKills(25);
        TeamInfoData team200 = new TeamInfoData();
        team200.setTeamId(200);
        team200.setWin(false);
        team200.setChampionKills(15);

        gameData.setMyData(myData);
        gameData.setGameInfoData(gameInfoData);
        gameData.setParticipantData(List.of(myData));
        gameData.setTeamInfoData(Map.of(100, team100, 200, team200));

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
        MatchRequest request = MatchRequest.builder().puuid("puuid-1234").queueId(420).pageNo(1).region("kr").build();
        List<String> matchIds = List.of("KR_123456789", "KR_987654321");
        given(matchService.findAllMatchIds(any(MatchRequest.class))).willReturn(matchIds);

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
        MatchRequest request = MatchRequest.builder().puuid("puuid-1234").queueId(420).pageNo(1).region("kr").build();
        GameData gameData = new GameData();
        ParticipantData myData = ParticipantData.builder().summonerName("MySummoner").championName("Ahri").kills(10).deaths(2).assists(5).win(true).build();
        gameData.setMyData(myData);
        MatchResponse matchResponse = new MatchResponse(List.of(gameData), 1L);

        given(matchService.getMatches(any(MatchRequest.class))).willReturn(matchResponse);

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
                                fieldWithPath("data.gameData[].myData.summonerName").type(JsonFieldType.STRING).description("내 소환사 이름"),
                                fieldWithPath("data.totalCount").type(JsonFieldType.NUMBER).description("총 매치 수"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)")
                        )
                ));
    }

    @DisplayName("랭크 챔피언 통계 조회 API")
    @Test
    void getRankChampions() throws Exception {
        // given
        MSChampionRequest request = new MSChampionRequest();
        request.setPuuid("puuid-1234");
        request.setSeason(2024);

        MSChampionDTO championResponse = new MSChampionDTO(
                266,
                "Aatrox",
                7.5,
                3.2,
                8.1,
                100.5,
                30.5,
                10L,
                15L
        );

        given(matchService.getRankChampions(any(MSChampionRequest.class))).willReturn(List.of(championResponse));

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

        TimelineData timelineData = new TimelineData();
        Map<Integer, Map<String, List<SeqTypeData>>> data = new HashMap<>();
        Map<String, List<SeqTypeData>> participantData = new HashMap<>();
        SeqTypeData itemSeq = new SeqTypeData();
        itemSeq.setId(1001);
        itemSeq.setType("ITEM_PURCHASED");
        itemSeq.setMinute(1L);
        participantData.put("ITEM_SEQ", List.of(itemSeq));
        data.put(1, participantData);
        timelineData.setData(data);

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
