package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.port.in.PostViewUseCase;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.application.port.out.PostViewDedupPort;
import com.example.lolserver.community.config.CommunityViewProperties;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.vo.ViewerKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 게시글 조회 기록 유스케이스.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다. 경로에 Redis 호출({@code tryMarkViewed})이 있어,
 * 트랜잭션으로 감싸면 Redis 장애 시 타임아웃 동안 DB 커넥션을 붙잡아 풀이 고갈된다
 * (ImageService 와 같은 규칙). 조회는 Spring Data 의 자체 읽기 트랜잭션, UPDATE 는
 * 리포지토리 메서드의 짧은 트랜잭션이 맡는다.
 */
@Service
@RequiredArgsConstructor
public class PostViewService implements PostViewUseCase {

    private final PostPersistencePort postPersistencePort;
    private final PostViewDedupPort postViewDedupPort;
    private final CommunityViewProperties properties;

    @Override
    public int recordView(Long postId, ViewerKey viewer) {
        Post post = postPersistencePort.findById(postId)
                .orElseThrow(() -> new CoreException(ErrorType.POST_NOT_FOUND));
        post.validateNotDeleted();

        if (postViewDedupPort.tryMarkViewed(postId, viewer, properties.getDedupTtl())) {
            postPersistencePort.incrementViewCount(postId);
            post.incrementViewCount();
        }
        return post.getViewCount();
    }
}
