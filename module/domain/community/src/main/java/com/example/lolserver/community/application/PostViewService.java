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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
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
