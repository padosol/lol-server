package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.web.CoreExceptionAdvice;
import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.port.in.PostQueryUseCase;
import com.example.lolserver.community.application.port.in.PostUseCase;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v1/community/posts 의 keyword 처리")
@ExtendWith(MockitoExtension.class)
class CommunityPostListKeywordTest {

    @Mock
    private PostUseCase postUseCase;

    @Mock
    private PostQueryUseCase postQueryUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CommunityPostController(postUseCase, postQueryUseCase))
                .setControllerAdvice(new CoreExceptionAdvice())
                .build();
    }

    @DisplayName("keyword 가 있으면 검색한다")
    @Test
    void keyword_검색() throws Exception {
        given(postQueryUseCase.searchPosts(any())).willReturn(new SliceResult<>(List.of(), false));

        mockMvc.perform(get("/api/v1/community/posts").param("keyword", "야스오"))
                .andExpect(status().isOk());

        ArgumentCaptor<PostSearchCommand> captor = ArgumentCaptor.forClass(PostSearchCommand.class);
        verify(postQueryUseCase).searchPosts(captor.capture());
        assertThat(captor.getValue().getKeyword()).isEqualTo("야스오");
        verify(postQueryUseCase, never()).getPosts(any());
    }

    @DisplayName("빈 keyword 는 검색이 아니라 카테고리·정렬 조건을 유지한 일반 목록이다")
    @Test
    void 빈keyword_목록() throws Exception {
        given(postQueryUseCase.getPosts(any())).willReturn(new SliceResult<>(List.of(), false));

        mockMvc.perform(get("/api/v1/community/posts")
                        .param("categoryId", "3")
                        .param("sort", "NEW")
                        .param("keyword", " "))
                .andExpect(status().isOk());

        ArgumentCaptor<PostSearchCommand> captor = ArgumentCaptor.forClass(PostSearchCommand.class);
        verify(postQueryUseCase).getPosts(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isEqualTo(3L);
        verify(postQueryUseCase, never()).searchPosts(any());
    }
}
