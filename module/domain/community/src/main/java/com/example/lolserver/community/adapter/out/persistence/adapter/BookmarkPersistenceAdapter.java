package com.example.lolserver.community.adapter.out.persistence.adapter;

import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import com.example.lolserver.community.application.model.readmodel.PostListReadModel;
import com.example.lolserver.community.adapter.out.persistence.mapper.CommunityBookmarkMapper;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBookmarkJpaRepository;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import lombok.RequiredArgsConstructor;
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
        CommunityBookmarkEntity saved = bookmarkJpaRepository.save(entity);
        return bookmarkMapper.toDomain(saved);
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
        Slice<CommunityPostEntity> slice =
                bookmarkJpaRepository.findBookmarkedPosts(memberId, pageable);

        return new SliceResult<>(
                slice.getContent().stream()
                        .map(entity -> PostListReadModel.builder()
                                .id(entity.getId())
                                .title(entity.getTitle())
                                .category(entity.getCategory())
                                .viewCount(entity.getViewCount())
                                .upvoteCount(entity.getUpvoteCount())
                                .downvoteCount(entity.getDownvoteCount())
                                .commentCount(entity.getCommentCount())
                                .hotScore(entity.getHotScore())
                                .authorId(entity.getMemberId())
                                .createdAt(entity.getCreatedAt())
                                .build())
                        .toList(),
                slice.hasNext()
        );
    }
}
