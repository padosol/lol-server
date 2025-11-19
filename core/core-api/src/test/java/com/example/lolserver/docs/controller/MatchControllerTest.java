package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.MatchController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.match.service.MatchService;
import com.example.lolserver.storage.db.core.repository.dto.data.GameData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.GameInfoData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.ParticipantData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.TeamInfoData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
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
        // Mock 데이터 설정
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
        gameData.setParticipantData(List.of(myData)); // 간단하게 myData만 포함
        gameData.setTeamInfoData(Map.of(100, team100, 200, team200));


        given(matchService.getGameData(anyString())).willReturn(gameData);

        // when & then
        mockMvc.perform(
                        get("/api/v1/matches/{matchId}", matchId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("match-get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("matchId").description("조회할 매치 ID")
                        ),
                        // 응답 필드가 너무 많으므로 relaxedResponseFields 사용
                        relaxedResponseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)"),

                                // GameData.myData
                                fieldWithPath("data.myData.summonerName").type(JsonFieldType.STRING).description("내 소환사 이름"),
                                fieldWithPath("data.myData.championName").type(JsonFieldType.STRING).description("내 챔피언 이름"),
                                fieldWithPath("data.myData.kills").type(JsonFieldType.NUMBER).description("내 Kills"),
                                fieldWithPath("data.myData.deaths").type(JsonFieldType.NUMBER).description("내 Deaths"),
                                fieldWithPath("data.myData.assists").type(JsonFieldType.NUMBER).description("내 Assists"),
                                fieldWithPath("data.myData.win").type(JsonFieldType.BOOLEAN).description("내 팀 승리 여부"),

                                // GameData.gameInfoData
                                fieldWithPath("data.gameInfoData.gameMode").type(JsonFieldType.STRING).description("게임 모드"),
                                fieldWithPath("data.gameInfoData.gameDuration").type(JsonFieldType.NUMBER).description("게임 시간 (초)"),

                                // GameData.participantData[]
                                fieldWithPath("data.participantData[].summonerName").type(JsonFieldType.STRING).description("참가자 소환사 이름"),
                                fieldWithPath("data.participantData[].championName").type(JsonFieldType.STRING).description("참가자 챔피언 이름"),
                                fieldWithPath("data.participantData[].teamId").type(JsonFieldType.NUMBER).description("참가자 팀 ID (100 or 200)"),
                                fieldWithPath("data.participantData[].win").type(JsonFieldType.BOOLEAN).description("참가자 팀 승리 여부"),

                                // GameData.teamInfoData
                                fieldWithPath("data.teamInfoData.100.win").type(JsonFieldType.BOOLEAN).description("블루팀 승리 여부"),
                                fieldWithPath("data.teamInfoData.100.championKills").type(JsonFieldType.NUMBER).description("블루팀 총 킬 수"),
                                fieldWithPath("data.teamInfoData.200.win").type(JsonFieldType.BOOLEAN).description("레드팀 승리 여부"),
                                fieldWithPath("data.teamInfoData.200.championKills").type(JsonFieldType.NUMBER).description("레드팀 총 킬 수")
                        )
                ));
    }
}
