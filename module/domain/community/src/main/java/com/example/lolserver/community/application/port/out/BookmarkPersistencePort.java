package com.example.lolserver.community.application.port.out;

import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.domain.Bookmark;

import java.util.Optional;

public interface BookmarkPersistencePort {

    Bookmark save(Bookmark bookmark);

    /**
     * 중복 북마크(409)와 미존재 북마크 해제(404)를 모두 이 한 번의 조회로 판정한다.
     * VotePersistencePort 의 findByMemberIdAndTargetTypeAndTargetId 와 같은 역할.
     */
    Optional<Bookmark> findByMemberIdAndPostId(Long memberId, Long postId);

    /**
     * 존재 여부만 필요할 때 쓴다. 엔티티를 통째로 읽어 즉시 버리지 않기 위함이며,
     * 특히 게시글 상세 조회는 최다 호출 읽기 경로다.
     */
    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    void delete(Bookmark bookmark);

    /**
     * 북마크한 게시글 목록. 북마크 시각 기준 최신순이며 삭제된 글은 제외한다.
     */
    SliceResult<PostListReadModel> findBookmarkedPosts(Long memberId, int page);
}
