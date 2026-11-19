package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;
import com.example.lolserver.esports.application.port.in.ContractEventUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.example.lolserver.esports.adapter.in.web.EsportsDocs.doc;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.CONTRACTED;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.EXPIRED;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.HIATUS;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.RENEWAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminContractEventController 테스트")
@ExtendWith(MockitoExtension.class)
class AdminContractEventControllerTest extends RestDocsSupport {

    private static final ContractEventResultModel CONTRACT_RESULT =
            new ContractEventResultModel("faker", CONTRACTED, List.of(EXPIRED, RENEWAL), List.of());
    private static final ContractEventResultModel EVENT_RESULT =
            new ContractEventResultModel("faker", CONTRACTED, List.of(), List.of(HIATUS));
    private static final String CONTRACT_BODY = """
            {"teamId": "t1", "role": "PLAYER", "position": "MID", "date": "2026-11-17", "endDate": "2029-11-19"}
            """;
    private static final String DATED_BODY = """
            {"date": "2026-11-20", "note": "공식 발표", "sourceUrl": "https://example.com/news"}
            """;

    @Mock
    private ContractEventUseCase contractEventUseCase;

    @Override
    protected Object initController() {
        return new AdminContractEventController(contractEventUseCase);
    }

    @DisplayName("신규 입단 API")
    @Test
    void sign_201() throws Exception {
        given(contractEventUseCase.sign(eq("faker"), any())).willReturn(CONTRACT_RESULT);

        perform("/signings", CONTRACT_BODY).andDo(doc("admin-esports-event-signing"));
    }

    @DisplayName("재계약 API — 만료 다음 날 시작이면 직전 계약을 그대로 이어 붙인다")
    @Test
    void renew_201() throws Exception {
        given(contractEventUseCase.renew(eq("faker"), any())).willReturn(CONTRACT_RESULT);

        perform("/renewals", CONTRACT_BODY)
                .andExpect(jsonPath("$.data.contracts[1].contractType").value("RENEWAL"))
                .andExpect(jsonPath("$.data.status.status").value("CONTRACTED"))
                .andDo(doc("admin-esports-event-renewal"));
    }

    @DisplayName("이적 API")
    @Test
    void transfer_201() throws Exception {
        given(contractEventUseCase.transfer(eq("faker"), any())).willReturn(CONTRACT_RESULT);

        perform("/transfers", CONTRACT_BODY).andDo(doc("admin-esports-event-transfer"));
    }

    @DisplayName("이적 — 효력 발생일이 없으면 400")
    @Test
    void transfer_효력발생일이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/esports/persons/{personId}/transfers", "faker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId": "gen", "role": "PLAYER", "position": "MID"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("팀 내 이동 API — 콜업·샌드다운·역할·포지션 변경")
    @Test
    void move_201() throws Exception {
        given(contractEventUseCase.move(eq("faker"), any())).willReturn(CONTRACT_RESULT);

        perform("/moves", """
                {"teamId": "t1", "role": "PLAYER", "position": "MID", "date": "2026-06-01"}
                """).andDo(doc("admin-esports-event-move"));
    }

    @DisplayName("임대 API")
    @Test
    void loan_201() throws Exception {
        given(contractEventUseCase.loan(eq("faker"), any())).willReturn(CONTRACT_RESULT);

        perform("/loans", """
                {"teamId": "gen", "date": "2026-11-20", "endDate": "2027-06-30"}
                """).andDo(doc("admin-esports-event-loan"));
    }

    @DisplayName("임대 조기 복귀 API")
    @Test
    void recall_201() throws Exception {
        given(contractEventUseCase.recall(eq("faker"), eq(12L), any())).willReturn(CONTRACT_RESULT);

        perform("/loans/12/recall", DATED_BODY).andDo(doc("admin-esports-event-recall"));
    }

    @DisplayName("방출·상호 해지 API")
    @Test
    void release_201() throws Exception {
        given(contractEventUseCase.release(eq("faker"), any())).willReturn(CONTRACT_RESULT);

        perform("/releases", DATED_BODY).andDo(doc("admin-esports-event-release"));
    }

    @DisplayName("은퇴 API")
    @Test
    void retire_201() throws Exception {
        given(contractEventUseCase.retire(eq("faker"), any())).willReturn(EVENT_RESULT);

        perform("/retirements", DATED_BODY).andDo(doc("admin-esports-event-retirement"));
    }

    @DisplayName("은퇴 번복 API")
    @Test
    void unretire_201() throws Exception {
        given(contractEventUseCase.unretire(eq("faker"), any())).willReturn(EVENT_RESULT);

        perform("/unretirements", DATED_BODY).andDo(doc("admin-esports-event-unretirement"));
    }

    @DisplayName("활동 중단 API — 사유: REST·SUSPENSION·MILITARY·PERSONAL")
    @Test
    void hiatus_201() throws Exception {
        given(contractEventUseCase.hiatus(eq("faker"), any())).willReturn(EVENT_RESULT);

        perform("/hiatuses", """
                {"date": "2026-11-20", "reason": "MILITARY", "note": "입대"}
                """)
                .andExpect(jsonPath("$.data.events[0].eventType").value("HIATUS"))
                .andDo(doc("admin-esports-event-hiatus"));
    }

    @DisplayName("활동 재개 API")
    @Test
    void comeBack_201() throws Exception {
        given(contractEventUseCase.comeBack(eq("faker"), any())).willReturn(EVENT_RESULT);

        perform("/returns", DATED_BODY).andDo(doc("admin-esports-event-return"));
    }

    private ResultActions perform(String path, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/esports/persons/{personId}" + path, "faker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.personId").value("faker"));
    }
}
