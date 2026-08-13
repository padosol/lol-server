package com.example.lolserver.community.adapter.out.persistence.repository;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityCategoryJpaRepository extends JpaRepository<CommunityCategoryEntity, Long> {

    /**
     * 비활성 카테고리도 함께 가져온다. 사이드바에서 숨기는 것과 기존 글의 배지 라벨을
     * 해석하는 것은 다른 일이고, 후자에는 숨겨진 카테고리도 필요하다.
     *
     * <p>정렬 키에 code 를 붙이는 이유는 그룹 쪽과 같다.
     */
    List<CommunityCategoryEntity> findAllByOrderByDisplayOrderAscCodeAsc();

    Optional<CommunityCategoryEntity> findByCode(String code);
}
