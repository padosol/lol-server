package com.example.lolserver.repository.community.dsl;

import com.example.lolserver.repository.community.dto.PostListDTO;
import com.example.lolserver.repository.community.dto.QPostListDTO;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.lolserver.repository.community.entity.QCommunityPostEntity.communityPostEntity;
import static com.example.lolserver.repository.member.entity.QMemberEntity.memberEntity;

@Repository
@RequiredArgsConstructor
public class CommunityPostRepositoryCustomImpl implements CommunityPostRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<PostListDTO> findPosts(String category, String sortType, LocalDateTime since, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<PostListDTO> result = jpaQueryFactory
                .select(new QPostListDTO(
                        communityPostEntity.id,
                        communityPostEntity.title,
                        communityPostEntity.category,
                        communityPostEntity.viewCount,
                        communityPostEntity.upvoteCount,
                        communityPostEntity.downvoteCount,
                        communityPostEntity.commentCount,
                        communityPostEntity.hotScore,
                        communityPostEntity.createdAt,
                        memberEntity.id,
                        memberEntity.nickname,
                        memberEntity.profileImageUrl
                ))
                .from(communityPostEntity)
                .join(memberEntity).on(memberEntity.id.eq(communityPostEntity.memberId))
                .where(
                        communityPostEntity.deleted.isFalse(),
                        categoryEq(category),
                        createdAfter(since)
                )
                .orderBy(resolveOrder(sortType))
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<PostListDTO> content = hasNext ? result.subList(0, pageSize) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public Slice<PostListDTO> searchPosts(String keyword, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<PostListDTO> result = jpaQueryFactory
                .select(new QPostListDTO(
                        communityPostEntity.id,
                        communityPostEntity.title,
                        communityPostEntity.category,
                        communityPostEntity.viewCount,
                        communityPostEntity.upvoteCount,
                        communityPostEntity.downvoteCount,
                        communityPostEntity.commentCount,
                        communityPostEntity.hotScore,
                        communityPostEntity.createdAt,
                        memberEntity.id,
                        memberEntity.nickname,
                        memberEntity.profileImageUrl
                ))
                .from(communityPostEntity)
                .join(memberEntity).on(memberEntity.id.eq(communityPostEntity.memberId))
                .where(
                        communityPostEntity.deleted.isFalse(),
                        keywordContains(keyword)
                )
                .orderBy(communityPostEntity.createdAt.desc())
                .offset((long) pageable.getPageNumber() * pageSize)
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = result.size() > pageSize;
        List<PostListDTO> content = hasNext ? result.subList(0, pageSize) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression categoryEq(String category) {
        return StringUtils.hasText(category) ? communityPostEntity.category.eq(category) : null;
    }

    private BooleanExpression createdAfter(LocalDateTime since) {
        return since != null ? communityPostEntity.createdAt.goe(since) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return communityPostEntity.title.containsIgnoreCase(keyword)
                .or(communityPostEntity.content.containsIgnoreCase(keyword));
    }

    private OrderSpecifier<?> resolveOrder(String sortType) {
        if ("HOT".equals(sortType)) {
            return communityPostEntity.hotScore.desc();
        }
        if ("TOP".equals(sortType)) {
            return communityPostEntity.upvoteCount.subtract(communityPostEntity.downvoteCount).desc();
        }
        return communityPostEntity.createdAt.desc();
    }
}
