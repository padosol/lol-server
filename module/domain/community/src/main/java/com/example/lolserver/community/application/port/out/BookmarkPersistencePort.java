package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.domain.Bookmark;

import java.util.Optional;

public interface BookmarkPersistencePort {

    Bookmark save(Bookmark bookmark);

    /**
     * 중복 북마크(409)와 미존재 북마크 해제(404)를 모두 이 한 번의 조회로 판정한다.
     * VotePersistencePort 의 findByMemberIdAndTargetTypeAndTargetId 와 같은 역할.
     */
    Optional<Bookmark> findByMemberIdAndPostId(Long memberId, Long postId);

    void delete(Bookmark bookmark);
}
