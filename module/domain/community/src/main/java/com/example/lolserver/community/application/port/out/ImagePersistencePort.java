package com.example.lolserver.community.application.port.out;

import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImagePersistencePort {

    PostImage save(PostImage image);

    List<PostImage> saveAll(List<PostImage> images);

    Optional<PostImage> findById(Long id);

    List<PostImage> findAllByIds(List<Long> ids);

    List<PostImage> findByPostId(Long postId);

    /**
     * 고아 판별. 상태와 경과시간만 본다 — 버킷 목록과 대조하지 않는다.
     *
     * @param threshold 이 시각 이전에 마지막으로 갱신된 행이 대상
     * @param limit     한 번에 처리할 최대 건수(배치가 한 번에 전부 물지 않도록)
     */
    List<PostImage> findExpired(ImageStatus status, LocalDateTime threshold, int limit);

    void deleteAllByIds(List<Long> ids);
}
