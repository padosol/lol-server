package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.application.port.out.PostViewDedupPort;
import com.example.lolserver.community.config.CommunityViewProperties;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.vo.ViewerKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostViewServiceTest {

    @Mock
    private PostPersistencePort postPersistencePort;

    @Mock
    private PostViewDedupPort postViewDedupPort;

    private PostViewService service;

    @BeforeEach
    void setUp() {
        service = new PostViewService(postPersistencePort, postViewDedupPort, new CommunityViewProperties());
    }

    private Post post(boolean deleted) {
        return Post.builder().id(1L).memberId(1L).viewCount(10).deleted(deleted).build();
    }

    @Test
    void recordView_첫조회면_증가하고_증가값을_반환한다() {
        ViewerKey viewer = ViewerKey.ofMember(7L);
        given(postPersistencePort.findById(1L)).willReturn(Optional.of(post(false)));
        given(postViewDedupPort.tryMarkViewed(1L, viewer, Duration.ofHours(24))).willReturn(true);

        assertThat(service.recordView(1L, viewer)).isEqualTo(11);
        then(postPersistencePort).should().incrementViewCount(1L);
    }

    @Test
    void recordView_중복이면_증가하지_않고_현재값을_반환한다() {
        ViewerKey viewer = ViewerKey.ofMember(7L);
        given(postPersistencePort.findById(1L)).willReturn(Optional.of(post(false)));
        given(postViewDedupPort.tryMarkViewed(1L, viewer, Duration.ofHours(24))).willReturn(false);

        assertThat(service.recordView(1L, viewer)).isEqualTo(10);
        then(postPersistencePort).should(never()).incrementViewCount(any());
    }

    @Test
    void recordView_없는_글이면_404() {
        given(postPersistencePort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordView(999L, ViewerKey.ofMember(7L)))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType()).isEqualTo(ErrorType.POST_NOT_FOUND);
        then(postViewDedupPort).shouldHaveNoInteractions();
    }

    @Test
    void recordView_삭제된_글이면_404_이고_중복판정을_하지_않는다() {
        given(postPersistencePort.findById(1L)).willReturn(Optional.of(post(true)));

        assertThatThrownBy(() -> service.recordView(1L, ViewerKey.ofMember(7L)))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType()).isEqualTo(ErrorType.POST_NOT_FOUND);
        then(postViewDedupPort).shouldHaveNoInteractions();
    }
}
