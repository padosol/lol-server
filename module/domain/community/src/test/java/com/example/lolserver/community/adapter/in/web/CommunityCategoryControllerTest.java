package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.community.application.model.readmodel.BoardGroupReadModel;
import com.example.lolserver.community.application.model.readmodel.CategoryReadModel;
import com.example.lolserver.community.application.model.readmodel.CategoryTreeReadModel;
import com.example.lolserver.community.application.port.in.CategoryQueryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityCategoryController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityCategoryControllerTest extends RestDocsSupport {

    @Mock
    private CategoryQueryUseCase categoryQueryUseCase;

    @Override
    protected Object initController() {
        return new CommunityCategoryController(categoryQueryUseCase);
    }

    @DisplayName("게시판 목록 조회 API")
    @Test
    void getCategories_게시판목록조회_성공() throws Exception {
        // given
        CategoryTreeReadModel tree = new CategoryTreeReadModel(List.of(
                new BoardGroupReadModel("COMMUNITY", "커뮤니티", 10, List.of(
                        new CategoryReadModel(3L, "GENERAL", "자유", null, 10, true, true, null),
                        new CategoryReadModel(4L, "HUMOR", "유머", null, 20, true, true, null))),
                new BoardGroupReadModel("ESPORTS", "e-스포츠", 20, List.of(
                        new CategoryReadModel(5L, "LCK", "LCK", null, 10, true, true, null)))
        ));

        given(categoryQueryUseCase.getCategoryTree("ko")).willReturn(tree);

        // when & then
        mockMvc.perform(
                        get("/api/community/categories")
                                .param("locale", "ko")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                // 배열 순서가 곧 화면 순서다. 프론트가 재정렬하지 않는 것이 계약이다.
                .andExpect(jsonPath("$.data.groups[0].code").value("COMMUNITY"))
                .andExpect(jsonPath("$.data.groups[0].categories[0].code").value("GENERAL"))
                // 글 작성·필터에 되돌려 보낼 값이라 id 가 반드시 실려야 한다
                .andExpect(jsonPath("$.data.groups[0].categories[0].id").value(3))
                .andExpect(jsonPath("$.data.groups[1].code").value("ESPORTS"))
                .andDo(document("community-category-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("locale").optional()
                                        .description("표시 로케일 (기본값 ko, 미지원 값은 ko 로 폴백)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.groups[]").type(JsonFieldType.ARRAY)
                                        .description("게시판 그룹 목록 (사이드바 노출 순서)"),
                                fieldWithPath("data.groups[].code").type(JsonFieldType.STRING)
                                        .description("그룹 코드"),
                                fieldWithPath("data.groups[].name").type(JsonFieldType.STRING)
                                        .description("그룹 표시 이름 (요청 로케일로 해석됨)"),
                                fieldWithPath("data.groups[].order").type(JsonFieldType.NUMBER)
                                        .description("그룹 노출 순서 (전역)"),
                                fieldWithPath("data.groups[].categories[]").type(JsonFieldType.ARRAY)
                                        .description("그룹에 속한 게시판 목록. 비어 있을 수 있다"),
                                fieldWithPath("data.groups[].categories[].id").type(JsonFieldType.NUMBER)
                                        .description("게시판 식별자. 글 작성·수정의 categoryId 와 목록 필터에 이 값을 보낸다"),
                                fieldWithPath("data.groups[].categories[].code").type(JsonFieldType.STRING)
                                        .description("게시판 코드. 표시·디버깅용이며 참조에는 id 를 쓴다"),
                                fieldWithPath("data.groups[].categories[].name").type(JsonFieldType.STRING)
                                        .description("게시판 표시 이름 (요청 로케일로 해석됨)"),
                                fieldWithPath("data.groups[].categories[].description")
                                        .type(JsonFieldType.STRING).optional()
                                        .description("게시판 설명. 없으면 null"),
                                fieldWithPath("data.groups[].categories[].order").type(JsonFieldType.NUMBER)
                                        .description("그룹 안에서의 순서. 그룹이 다르면 값이 겹칠 수 있다"),
                                fieldWithPath("data.groups[].categories[].visible").type(JsonFieldType.BOOLEAN)
                                        .description("false 면 사이드바·필터에서 숨긴다. 라벨 해석에는 그대로 쓴다"),
                                fieldWithPath("data.groups[].categories[].writable").type(JsonFieldType.BOOLEAN)
                                        .description("false 면 글을 쓸 수 없다 (공지 등 읽기 전용)"),
                                fieldWithPath("data.groups[].categories[].icon")
                                        .type(JsonFieldType.STRING).optional()
                                        .description("아이콘 식별자. 현재는 항상 null")
                        )
                ));
    }
}
