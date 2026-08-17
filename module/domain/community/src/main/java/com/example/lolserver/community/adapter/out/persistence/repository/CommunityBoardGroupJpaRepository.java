package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBoardGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityBoardGroupJpaRepository extends JpaRepository<CommunityBoardGroupEntity, Long> {

    /**
     * 2차 정렬 키 code 가 필수다. display_order 에 UNIQUE 를 걸지 않으므로 값이 같을 수
     * 있고, 그러면 반환 순서가 비결정적이 되어 배포마다 사이드바 순서가 뒤바뀐다.
     */
    List<CommunityBoardGroupEntity> findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
}
