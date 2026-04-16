package com.example.lolserver.repository.duo.dsl;

import com.example.lolserver.repository.duo.dto.DuoPostListDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DuoPostRepositoryCustom {

    Slice<DuoPostListDTO> findActivePosts(String lane, String tier, Pageable pageable);

    Slice<DuoPostListDTO> findByMemberId(Long memberId, Pageable pageable);
}
