package com.example.lolserver.summoner.adapter.in.web;

import com.example.lolserver.common.web.CoreExceptionAdvice;
import com.example.lolserver.summoner.application.port.in.LeagueQueryUseCase;
import com.example.lolserver.summoner.application.port.in.SpectatorQueryUseCase;
import com.example.lolserver.summoner.application.port.in.SummonerQueryUseCase;
import com.example.lolserver.summoner.application.port.in.SummonerUseCase;
import com.example.lolserver.summoner.domain.SummonerRenewal;
import com.example.lolserver.summoner.domain.vo.GameName;
import com.example.lolserver.shared.RenewalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 새 경로와 옛 경로가 병행되는 동안 같은 모양의 URL 이 의도한 핸들러로 가는지 검증한다.
 * Spring 은 변수 세그먼트가 적은 패턴을 우선하므로, 새 {@code /summoners/{puuid}/xxx} 가
 * 옛 {@code /summoners/{platformId}/{gameName}} 보다 먼저 선택돼야 한다.
 */
@DisplayName("summoner 새·옛 경로 매핑 우선순위")
@ExtendWith(MockitoExtension.class)
class SummonerRouteMappingTest {

    @Mock
    private SummonerQueryUseCase summonerQueryUseCase;

    @Mock
    private SummonerUseCase summonerUseCase;

    @Mock
    private LeagueQueryUseCase leagueQueryUseCase;

    @Mock
    private SpectatorQueryUseCase spectatorQueryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SummonerController(summonerQueryUseCase, summonerUseCase),
                        new LeagueController(leagueQueryUseCase),
                        new SpectatorController(spectatorQueryUseCase))
                .setControllerAdvice(new CoreExceptionAdvice())
                .build();
    }

    @DisplayName("/summoners/{puuid}/leagues 는 옛 /summoners/{platformId}/{gameName} 이 아니라 리그 조회로 간다")
    @Test
    void leagues_우선() throws Exception {
        given(leagueQueryUseCase.getLeaguesBypuuid("puuid-1")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/summoners/puuid-1/leagues"))
                .andExpect(status().isOk());

        verify(leagueQueryUseCase).getLeaguesBypuuid("puuid-1");
        verify(summonerQueryUseCase, never()).getSummoner(any(), any());
    }

    @DisplayName("/summoners/{puuid}/active-game 은 관전 조회로 간다")
    @Test
    void activeGame_우선() throws Exception {
        mockMvc.perform(get("/api/v1/summoners/puuid-1/active-game"))
                .andExpect(status().isOk());

        verify(spectatorQueryUseCase).getCurrentGameInfo("puuid-1");
        verify(summonerQueryUseCase, never()).getSummoner(any(), any());
    }

    @DisplayName("GET /summoners/{puuid}/renewal 은 갱신 상태 조회로 간다")
    @Test
    void renewalStatus_우선() throws Exception {
        given(summonerQueryUseCase.renewalSummonerStatus("puuid-1"))
                .willReturn(new SummonerRenewal("puuid-1", RenewalStatus.PROGRESS));

        mockMvc.perform(get("/api/v1/summoners/puuid-1/renewal"))
                .andExpect(status().isOk());

        verify(summonerQueryUseCase).renewalSummonerStatus("puuid-1");
        verify(summonerQueryUseCase, never()).getSummoner(any(), any());
    }

    @DisplayName("POST /summoners/{puuid}/renewal 은 저장된 플랫폼으로 갱신을 요청하고 202 를 준다")
    @Test
    void renewalRequest_202() throws Exception {
        given(summonerUseCase.renewalSummonerInfo("puuid-1"))
                .willReturn(new SummonerRenewal("puuid-1", RenewalStatus.SUCCESS));

        mockMvc.perform(post("/api/v1/summoners/puuid-1/renewal"))
                .andExpect(status().isAccepted());
    }

    @DisplayName("/summoners/by-riot-id/{gameName} 은 platform query 로 Riot ID 조회를 한다")
    @Test
    void byRiotId() throws Exception {
        mockMvc.perform(get("/api/v1/summoners/by-riot-id/hide-KR1").param("platform", "kr"))
                .andExpect(status().isOk());

        verify(summonerQueryUseCase).getSummoner(any(GameName.class), eq("kr"));
    }

    @DisplayName("/summoners/autocomplete 는 puuid 조회가 아니라 자동완성으로 간다")
    @Test
    void autocomplete_우선() throws Exception {
        mockMvc.perform(get("/api/v1/summoners/autocomplete").param("q", "hide").param("platform", "kr"))
                .andExpect(status().isOk());

        verify(summonerQueryUseCase).getAllSummonerAutoComplete("hide", "kr");
        verify(summonerQueryUseCase, never()).getSummonerByPuuid(any());
    }

    @DisplayName("새 경로에서 필수 query platform 이 빠지면 400")
    @Test
    void platform_누락_400() throws Exception {
        mockMvc.perform(get("/api/v1/summoners/autocomplete").param("q", "hide"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/summoners/by-riot-id/hide-KR1"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("옛 /summoners/{platformId}/{gameName} 은 계속 Riot ID 조회로 간다")
    @Test
    void 옛경로_유지() throws Exception {
        mockMvc.perform(get("/api/v1/summoners/kr/hide-KR1"))
                .andExpect(status().isOk());

        verify(summonerQueryUseCase).getSummoner(any(GameName.class), eq("kr"));
    }
}
