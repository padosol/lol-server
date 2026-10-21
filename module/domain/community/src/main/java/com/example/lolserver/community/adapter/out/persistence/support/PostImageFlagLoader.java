package com.example.lolserver.community.adapter.out.persistence.support;

import com.example.lolserver.community.adapter.out.persistence.repository.CommunityImageJpaRepository;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.domain.vo.ImageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 목록의 {@code hasImage} 를 페이지 단위로 한 번에 채운다.
 *
 * <p>작성자(author)와 달리 <b>영속성 계층에서</b> 채우는 이유는 이미지가 같은 컨텍스트,
 * 같은 DB 에 있기 때문이다. author 가 애플리케이션 서비스로 올라간 것은 member 가 다른
 * 컨텍스트라 조인할 수 없어서였지, 보강이라는 행위 자체 때문이 아니다.
 *
 * <p>목록 프로젝션에 {@code EXISTS} 서브쿼리를 넣지 않은 이유: 목록을 만드는 경로가
 * QueryDSL(목록·검색), JPQL(북마크), 엔티티 조회(내 글)로 셋이라 같은 조건을 세 문법으로
 * 세 벌 쓰게 된다. 여기로 모으면 한 벌이고, 셋 다 같은 답을 낸다.
 */
@Component
@RequiredArgsConstructor
public class PostImageFlagLoader {

    private final CommunityImageJpaRepository imageJpaRepository;

    /**
     * 이미지가 붙어 있는 글에 표시를 남긴 새 목록을 돌려준다. 입력은 건드리지 않는다.
     */
    public List<PostListReadModel> markPostsWithImage(List<PostListReadModel> posts) {
        if (posts.isEmpty()) {
            return posts;
        }

        List<Long> postIds = posts.stream()
                .map(PostListReadModel::getId)
                .toList();

        // ATTACHED 만 센다. 올리다 만(UPLOADING·PENDING) 이미지나 글에서 뺀(DETACHED)
        // 이미지는 본문에 보이지 않으므로, 그것까지 세면 사진 없는 글에 아이콘이 붙는다.
        Set<Long> withImage = Set.copyOf(imageJpaRepository.findPostIdsByPostIdInAndStatus(
                postIds, ImageStatus.ATTACHED.name()));

        return posts.stream()
                .map(post -> post.toBuilder()
                        .hasImage(withImage.contains(post.getId()))
                        .build())
                .toList();
    }
}
