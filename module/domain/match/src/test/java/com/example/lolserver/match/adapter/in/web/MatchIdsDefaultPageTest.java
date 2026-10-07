package com.example.lolserver.match.adapter.in.web;

import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.web.CoreExceptionAdvice;
import com.example.lolserver.match.application.command.MatchCommand;
import com.example.lolserver.match.application.port.in.MatchQueryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v1/summoners/{puuid}/match-ids 기본 페이지")
@ExtendWith(MockitoExtension.class)
class MatchIdsDefaultPageTest {

    @Mock
    private MatchQueryUseCase matchQueryUseCase;

    @DisplayName("page 를 생략하면 500 이 아니라 0페이지로 조회한다")
    @Test
    void page생략_0페이지() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MatchController(matchQueryUseCase))
                .setControllerAdvice(new CoreExceptionAdvice())
                .build();
        given(matchQueryUseCase.findAllMatchIds(any())).willReturn(new SliceResult<>(List.of(), false));

        mockMvc.perform(get("/api/v1/summoners/puuid-1/match-ids"))
                .andExpect(status().isOk());

        ArgumentCaptor<MatchCommand> captor = ArgumentCaptor.forClass(MatchCommand.class);
        verify(matchQueryUseCase).findAllMatchIds(captor.capture());
        assertThat(captor.getValue().getPageNo()).isZero();
        assertThat(captor.getValue().getPuuid()).isEqualTo("puuid-1");
    }
}
