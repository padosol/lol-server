package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.common.web.CoreExceptionAdvice;
import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;
import com.example.lolserver.esports.application.port.in.ContractCorrectionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static com.example.lolserver.esports.adapter.in.web.EsportsDocs.doc;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.EXPIRED;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.RENEWAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminCorrectionController 테스트")
@ExtendWith(MockitoExtension.class)
class AdminCorrectionControllerTest extends RestDocsSupport {

    private static final String CORRECT_BODY = """
            {"startDate": "2026-11-17", "endDate": "2029-11-19", "position": "MID", "note": "종료일 정정"}
            """;

    @Mock
    private ContractCorrectionUseCase contractCorrectionUseCase;

    @Override
    protected Object initController() {
        return new AdminCorrectionController(contractCorrectionUseCase);
    }

    @DisplayName("계약 원본 목록 API")
    @Test
    void getContracts_200() throws Exception {
        given(contractCorrectionUseCase.getContracts("faker")).willReturn(List.of(EXPIRED, RENEWAL));

        mockMvc.perform(get("/api/v1/admin/esports/persons/{personId}/contracts", "faker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].contractId").value(10))
                .andDo(doc("admin-esports-contract-list"));
    }

    @DisplayName("계약 정정 API — 보낸 값으로 덮어쓴다")
    @Test
    void correctContract_200() throws Exception {
        given(contractCorrectionUseCase.correctContract(eq(11L), any())).willReturn(RENEWAL);

        mockMvc.perform(patch("/api/v1/admin/esports/contracts/{contractId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORRECT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endDate").value("2029-11-19"))
                .andDo(doc("admin-esports-contract-correct"));
    }

    @DisplayName("계약 정정 — 다른 계약과 겹치면 409")
    @Test
    void correctContract_겹치면_409() throws Exception {
        given(contractCorrectionUseCase.correctContract(eq(11L), any()))
                .willThrow(new CoreException(ErrorType.CONTRACT_PERIOD_CONFLICT));
        MockMvc withAdvice = MockMvcBuilders.standaloneSetup(initController())
                .setControllerAdvice(new CoreExceptionAdvice())
                .build();

        withAdvice.perform(patch("/api/v1/admin/esports/contracts/{contractId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORRECT_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage.errorCode").value("E409"));
    }

    @DisplayName("계약 삭제 API")
    @Test
    void deleteContract_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/esports/contracts/{contractId}", 11L))
                .andExpect(status().isNoContent())
                .andDo(doc("admin-esports-contract-delete"));

        verify(contractCorrectionUseCase).deleteContract(11L);
    }

    @DisplayName("커리어 사건 삭제 API")
    @Test
    void deleteCareerEvent_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/esports/career-events/{eventId}", 3L))
                .andExpect(status().isNoContent())
                .andDo(doc("admin-esports-career-event-delete"));

        verify(contractCorrectionUseCase).deleteCareerEvent(3L);
    }

    @DisplayName("검증 결과 API — 0행이어야 정상")
    @Test
    void getRosterIssues_200() throws Exception {
        given(contractCorrectionUseCase.getRosterIssues()).willReturn(List.of(
                new RosterIssueReadModel("MOVE_NOT_CONTINUOUS", "faker", 11L, "t1", LocalDate.of(2026, 6, 1))));

        mockMvc.perform(get("/api/v1/admin/esports/roster-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].problem").value("MOVE_NOT_CONTINUOUS"))
                .andDo(doc("admin-esports-roster-issues"));
    }
}
