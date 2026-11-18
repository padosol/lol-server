package com.example.lolserver.community.adapter.out.persistence;

import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityPostEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityPostJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostViewService 는 Redis 호출을 트랜잭션 밖에 두기 위해 트랜잭션이 없다.
 * 그래서 UPDATE 가 호출자 트랜잭션 없이도 스스로 트랜잭션을 열어 동작해야 한다.
 */
class PostViewCountPersistenceTest extends RepositoryTestBase {

    @Autowired
    private CommunityPostJpaRepository postJpaRepository;

    @DisplayName("incrementViewCount 는 바깥 트랜잭션이 없어도 조회수를 1 올린다")
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void incrementViewCount_withoutSurroundingTransaction() {
        LocalDateTime now = LocalDateTime.now();
        Long postId = postJpaRepository.save(CommunityPostEntity.builder()
                .memberId(99L).title("글").content("내용").categoryId(1L)
                .deleted(false).createdAt(now).updatedAt(now).build()).getId();
        try {
            int before = postJpaRepository.findById(postId).orElseThrow().getViewCount();

            postJpaRepository.incrementViewCount(postId);

            assertThat(postJpaRepository.findById(postId).orElseThrow().getViewCount())
                    .isEqualTo(before + 1);
        } finally {
            postJpaRepository.deleteById(postId);
        }
    }
}
