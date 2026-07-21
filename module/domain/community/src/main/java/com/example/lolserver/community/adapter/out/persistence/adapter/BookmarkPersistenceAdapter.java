package com.example.lolserver.community.adapter.out.persistence.adapter;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBookmarkEntity;
import com.example.lolserver.community.adapter.out.persistence.mapper.CommunityBookmarkMapper;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBookmarkJpaRepository;
import com.example.lolserver.community.application.port.out.BookmarkPersistencePort;
import com.example.lolserver.community.domain.Bookmark;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookmarkPersistenceAdapter implements BookmarkPersistencePort {

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
}
