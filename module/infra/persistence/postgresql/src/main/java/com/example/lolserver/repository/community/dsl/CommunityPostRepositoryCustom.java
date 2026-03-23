package com.example.lolserver.repository.community.dsl;

import com.example.lolserver.repository.community.dto.PostListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface CommunityPostRepositoryCustom {

    Slice<PostListDTO> findPosts(String category, String sortType, LocalDateTime since, Pageable pageable);

    Slice<PostListDTO> searchPosts(String keyword, Pageable pageable);
}
