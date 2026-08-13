package com.example.lolserver.community.adapter.out.persistence.dsl;

import com.example.lolserver.community.adapter.out.persistence.dto.PostListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface CommunityPostRepositoryCustom {

    /**
     * @param categoryId 게시판 필터. null 이면 전체. 어댑터가 code 를 미리 해석해 넘긴다 —
     *                   여기서 조인으로 code 를 걸면 idx_cp_cat_hot 의 선두 컬럼을 놓친다.
     */
    Slice<PostListDTO> findPosts(Long categoryId, String sortType, LocalDateTime since, Pageable pageable);

    Slice<PostListDTO> searchPosts(String keyword, Pageable pageable);
}
