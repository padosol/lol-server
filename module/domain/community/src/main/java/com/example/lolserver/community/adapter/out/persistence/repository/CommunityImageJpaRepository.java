package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityImageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CommunityImageJpaRepository extends JpaRepository<CommunityImageEntity, Long> {

    List<CommunityImageEntity> findAllByIdIn(Collection<Long> ids);

    List<CommunityImageEntity> findAllByPostId(Long postId);

    /**
     * 정리 배치용 스캔. {@code idx_ci_status_updated} 가 정확히 이 조회를 위해 존재한다.
     *
     * <p>버킷을 {@code ListObjects} 로 훑어 DB 와 대조하지 않는 이유는, 객체 수에 비례해 비싼 것도
     * 있지만 무엇보다 <b>"방금 올라간 정상 파일"과 "고아"를 구분할 수 없기 때문</b>이다.
     * 목록에는 둘 다 그냥 객체로 보인다. 상태와 경과시간만 보면 된다.
     *
     * <p>기준이 {@code created_at} 이 아니라 {@code updated_at} 인 이유: DETACHED 유예는
     * "떨어져 나온 시점"부터 세야 한다. 1년 전에 올린 이미지를 오늘 글에서 뺐는데
     * {@code created_at} 으로 재면 즉시 삭제 대상이 된다.
     */
    List<CommunityImageEntity> findAllByStatusAndUpdatedAtBefore(
            String status, LocalDateTime threshold, Pageable pageable);
}
