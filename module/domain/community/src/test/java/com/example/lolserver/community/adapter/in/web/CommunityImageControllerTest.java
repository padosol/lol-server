package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.common.test.TestAuthenticatedMemberResolver;
import com.example.lolserver.community.application.command.UploadImageCommand;
import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;
import com.example.lolserver.community.application.port.in.ImageUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityImageController 테스트")
@ExtendWith(MockitoExtension.class)
class CommunityImageControllerTest extends RestDocsSupport {

    @Mock
    private ImageUseCase imageUseCase;

    @Override
    protected Object initController() {
        return new CommunityImageController(imageUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[] {new TestAuthenticatedMemberResolver()};
    }

    @DisplayName("이미지 업로드 API")
    @Test
    void uploadImage() throws Exception {
        given(imageUseCase.upload(anyLong(), any(UploadImageCommand.class)))
                .willReturn(PostImageReadModel.builder()
                        .imageId(1042L)
                        .url("https://cdn.example.com/local/community/2026/08/9f2c.webp")
                        .width(1920)
                        .height(1080)
                        .sizeBytes(481203L)
                        .build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png",
                "fake-image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/community/images").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageId").value(1042))
                .andDo(document("community-image-upload",
                        preprocessResponse(prettyPrint()),
                        requestParts(
                                partWithName("file").description("업로드할 이미지 파일 (jpeg/png/gif/webp, 최대 5MB)")),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("응답 결과"),
                                fieldWithPath("data.imageId").type(JsonFieldType.NUMBER).description("이미지 식별자. 글 저장 시 imageIds 에 담아 보낸다"),
                                fieldWithPath("data.url").type(JsonFieldType.STRING).description("CDN 영구 URL (presigned 아님). 본문에 그대로 삽입한다"),
                                fieldWithPath("data.width").type(JsonFieldType.NUMBER).description("저장된 이미지 폭 (상한 초과 시 축소된 값)"),
                                fieldWithPath("data.height").type(JsonFieldType.NUMBER).description("저장된 이미지 높이"),
                                fieldWithPath("data.sizeBytes").type(JsonFieldType.NUMBER).description("저장된 바이트 수"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("오류 정보")
                        )));
    }

    @DisplayName("업로드 요청의 선언 Content-Type 은 커맨드에 담기지만 검증에는 쓰이지 않는다")
    @Test
    void uploadImage_carriesDeclaredContentType() throws Exception {
        given(imageUseCase.upload(anyLong(), any(UploadImageCommand.class)))
                .willReturn(PostImageReadModel.builder().imageId(1L).url("https://cdn/x.png").build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/community/images").file(file))
                .andExpect(status().isCreated());

        ArgumentCaptor<UploadImageCommand> captor =
                ArgumentCaptor.forClass(UploadImageCommand.class);
        then(imageUseCase).should().upload(anyLong(), captor.capture());
        assertThat(captor.getValue().getDeclaredContentType()).isEqualTo("image/png");
        assertThat(captor.getValue().getContent()).isEqualTo(new byte[] {1, 2, 3});
    }

    @DisplayName("이미지 삭제 API")
    @Test
    void deleteImage() throws Exception {
        mockMvc.perform(delete("/api/community/images/{imageId}", 1042L))
                .andExpect(status().isNoContent())
                .andDo(document("community-image-delete",
                        pathParameters(
                                parameterWithName("imageId").description("삭제할 이미지 식별자 (본인 소유 + 미첨부 상태만)"))));

        then(imageUseCase).should().delete(1L, 1042L);
    }
}
