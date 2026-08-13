package com.example.lolserver.community.adapter.out.persistence.adapter;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import com.example.lolserver.community.adapter.out.persistence.dto.PostListDTO;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.adapter.out.persistence.mapper.CommunityBookmarkMapper;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBookmarkJpaRepository;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookmarkPersistenceAdapter implements BookmarkPersistencePort {

    private static final int PAGE_SIZE = 20;

    private final CommunityBookmarkJpaRepository bookmarkJpaRepository;
    private final CommunityBookmarkMapper bookmarkMapper;

    @Override
    public Bookmark save(Bookmark bookmark) {
        CommunityBookmarkEntity entity = bookmarkMapper.toEntity(bookmark);
        try {
            CommunityBookmarkEntity saved = bookmarkJpaRepository.save(entity);
            return bookmarkMapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            // 서비스의 사전 조회를 동시 요청 두 건이 모두 통과하면 여기서 uq_cb_member_post 가
            // 걸린다. 그대로 두면 처리되지 않은 예외로 500 이 나가므로, 사전 조회가 잡았을
            // 때와 같은 도메인 에러로 수렴시킨다.
            throw new CoreException(ErrorType.BOOKMARK_ALREADY_EXISTS);
        }
    }

    @Override
    public boolean existsByMemberIdAndPostId(Long memberId, Long postId) {
        return bookmarkJpaRepository.existsByMemberIdAndPostId(memberId, postId);
    }

    @Override
    public Optional<Bookmark> findByMemberIdAndPostId(Long memberId, Long postId) {
        return bookmarkJpaRepository.findByMemberIdAndPostId(memberId, postId)
                .map(bookmarkMapper::toDomain);
    }

    @Override
    public void delete(Bookmark bookmark) {
        bookmarkJpaRepository.deleteById(bookmark.getId());
    }

    @Override
    public SliceResult<PostListReadModel> findBookmarkedPosts(Long memberId, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<PostListDTO> slice =
                bookmarkJpaRepository.findBookmarkedPosts(memberId, pageable);

        return new SliceResult<>(
                slice.getContent().stream()
                        .map(dto -> PostListReadModel.builder()
                                .id(dto.getId())
                                .title(dto.getTitle())
                                .categoryId(dto.getCategoryId())
                                .viewCount(dto.getViewCount())
                                .upvoteCount(dto.getUpvoteCount())
                                .downvoteCount(dto.getDownvoteCount())
                                .commentCount(dto.getCommentCount())
                                .hotScore(dto.getHotScore())
                                .authorId(dto.getMemberId())
                                .createdAt(dto.getCreatedAt())
                                .build())
                        .toList(),
                slice.hasNext()
        );
    }
}
