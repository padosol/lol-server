package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.version.VersionController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.version.application.VersionService;
import com.example.lolserver.domain.version.application.model.VersionReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("VersionController 테스트")
@ExtendWith(MockitoExtension.class)
class VersionControllerTest extends RestDocsSupport {

    @Mock
    private VersionService versionService;

    @Override
    protected Object initController() {
        return new VersionController(versionService);
    }

    @DisplayName("최신 버전 조회 API")
    @Test
    void getLatestVersion_성공() throws Exception {
        // given
        VersionReadModel latestVersion = new VersionReadModel(
                3L, "14.24.1", LocalDateTime.of(2024, 12, 11, 10, 0)
        );

        given(versionService.getLatestVersion()).willReturn(latestVersion);

        // when & then
        mockMvc.perform(
                        get("/api/v1/versions/latest")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("version-latest",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.versionId").type(JsonFieldType.NUMBER)
                                        .description("버전 ID"),
                                fieldWithPath("data.versionValue").type(JsonFieldType.STRING)
                                        .description("버전 값 (e.g., 14.24.1)"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("버전 생성 시간")
                        )
                ));
    }

    @DisplayName("전체 버전 목록 조회 API")
    @Test
    void getAllVersions_성공() throws Exception {
        // given
        List<VersionReadModel> versions = List.of(
                new VersionReadModel(3L, "14.24.1", LocalDateTime.of(2024, 12, 11, 10, 0)),
                new VersionReadModel(2L, "14.23.1", LocalDateTime.of(2024, 11, 27, 10, 0)),
                new VersionReadModel(1L, "14.22.1", LocalDateTime.of(2024, 11, 13, 10, 0))
        );

        given(versionService.getAllVersions()).willReturn(versions);

        // when & then
        mockMvc.perform(
                        get("/api/v1/versions")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("version-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("버전 목록 (최신순 정렬)"),
                                fieldWithPath("data[].versionId").type(JsonFieldType.NUMBER)
                                        .description("버전 ID"),
                                fieldWithPath("data[].versionValue").type(JsonFieldType.STRING)
                                        .description("버전 값 (e.g., 14.24.1)"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING)
                                        .description("버전 생성 시간")
                        )
                ));
    }

    @DisplayName("특정 버전 조회 API - 버전 존재")
    @Test
    void getVersionById_버전존재_성공() throws Exception {
        // given
        Long versionId = 1L;
        VersionReadModel version = new VersionReadModel(
                versionId, "14.22.1", LocalDateTime.of(2024, 11, 13, 10, 0)
        );

        given(versionService.getVersionById(versionId)).willReturn(version);

        // when & then
        mockMvc.perform(
                        get("/api/v1/versions/{versionId}", versionId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("version-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("versionId").description("조회할 버전 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.versionId").type(JsonFieldType.NUMBER)
                                        .description("버전 ID"),
                                fieldWithPath("data.versionValue").type(JsonFieldType.STRING)
                                        .description("버전 값 (e.g., 14.22.1)"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("버전 생성 시간")
                        )
                ));
    }

    @DisplayName("특정 버전 조회 API - 버전 미존재")
    @Test
    void getVersionById_버전미존재_null반환() throws Exception {
        // given
        Long versionId = 999L;

        given(versionService.getVersionById(versionId)).willReturn(null);

        // when & then
        mockMvc.perform(
                        get("/api/v1/versions/{versionId}", versionId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("version-by-id-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("versionId").description("조회할 버전 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("버전이 존재하지 않는 경우 null")
                        )
                ));
    }
}
