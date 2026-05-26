package com.example.lolserver.community.adapter.out.persistence.adapter;

import com.example.lolserver.community.application.command.PostSearchCommand;
import com.example.lolserver.community.application.model.PostListReadModel;
import com.example.lolserver.community.application.port.out.PostPersistencePort;
import com.example.lolserver.community.domain.Post;
import com.example.lolserver.community.domain.vo.SortType;
import com.example.lolserver.community.domain.vo.TimePeriod;
import com.example.lolserver.community.adapter.out.persistence.dto.PostListDTO;
import com.example.lolserver.community.adapter.out.persistence.dsl.CommunityPostRepositoryCustom;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import com.example.lolserver.community.adapter.out.persistence.mapper.CommunityPostMapper;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityPostJpaRepository;
import com.example.lolserver.common.support.SliceResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements PostPersistencePort {

    private static final int PAGE_SIZE = 20;

    private final CommunityPostJpaRepository postJpaRepository;
    private final CommunityPostRepositoryCustom postRepositoryCustom;
    private final CommunityPostMapper postMapper;

    @Override
    public Post save(Post post) {
        CommunityPostEntity entity = postMapper.toEntity(post);
        CommunityPostEntity saved = postJpaRepository.save(entity);
        return postMapper.toDomain(saved);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id)
                .map(postMapper::toDomain);
    }

    @Override
    public SliceResult<PostListReadModel> findPosts(PostSearchCommand command) {
        Pageable pageable = PageRequest.of(command.getPage(), PAGE_SIZE);
        String sortType = command.getSortType() != null ? command.getSortType().name() : SortType.HOT.name();
        LocalDateTime since = resolveSince(command.getTimePeriod());

        Slice<PostListDTO> slice = postRepositoryCustom.findPosts(
                command.getCategory(), sortType, since, pageable);

        return toPage(slice);
    }

    @Override
    public SliceResult<PostListReadModel> searchPosts(PostSearchCommand command) {
        Pageable pageable = PageRequest.of(command.getPage(), PAGE_SIZE);

        Slice<PostListDTO> slice = postRepositoryCustom.searchPosts(command.getKeyword(), pageable);

        return toPage(slice);
    }

    @Override
    public SliceResult<PostListReadModel> findByMemberId(
            Long memberId, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Slice<CommunityPostEntity> slice = postJpaRepository
                .findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(
                        memberId, pageable);

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

    @Override
    public void incrementViewCount(Long postId) {
        postJpaRepository.incrementViewCount(postId);
    }

    @Override
    public void updateVoteCounts(Long postId, int upvoteCount, int downvoteCount) {
        postJpaRepository.updateVoteCounts(postId, upvoteCount, downvoteCount);
    }

    @Override
    public void updateCommentCount(Long postId, int commentCount) {
        postJpaRepository.updateCommentCount(postId, commentCount);
    }

    @Override
    public void updateHotScore(Long postId, double hotScore) {
        postJpaRepository.updateHotScore(postId, hotScore);
    }

    private LocalDateTime resolveSince(TimePeriod period) {
        if (period == null || period == TimePeriod.ALL) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case DAILY -> now.minusDays(1);
            case WEEKLY -> now.minusWeeks(1);
            case MONTHLY -> now.minusMonths(1);
            case ALL -> null;
        };
    }

    private SliceResult<PostListReadModel> toPage(Slice<PostListDTO> slice) {
        return new SliceResult<>(
                slice.getContent().stream()
                        .map(dto -> PostListReadModel.builder()
                                .id(dto.getId())
                                .title(dto.getTitle())
                                .category(dto.getCategory())
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
