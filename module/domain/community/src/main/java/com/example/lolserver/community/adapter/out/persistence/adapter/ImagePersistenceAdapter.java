package com.example.lolserver.community.adapter.out.persistence.adapter;

import com.example.lolserver.community.adapter.out.persistence.entity.CommunityImageEntity;
import com.example.lolserver.community.adapter.out.persistence.mapper.CommunityImageMapper;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityImageJpaRepository;
import com.example.lolserver.community.application.port.out.ImagePersistencePort;
import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImagePersistenceAdapter implements ImagePersistencePort {

    private final CommunityImageJpaRepository imageJpaRepository;
    private final CommunityImageMapper imageMapper;

    @Override
    public PostImage save(PostImage image) {
        CommunityImageEntity saved = imageJpaRepository.save(imageMapper.toEntity(image));
        return imageMapper.toDomain(saved);
    }

    @Override
    public List<PostImage> saveAll(List<PostImage> images) {
        List<CommunityImageEntity> entities = images.stream()
                .map(imageMapper::toEntity)
                .toList();
        return imageJpaRepository.saveAll(entities).stream()
                .map(imageMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PostImage> findById(Long id) {
        return imageJpaRepository.findById(id).map(imageMapper::toDomain);
    }

    @Override
    public List<PostImage> findAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return imageJpaRepository.findAllByIdIn(ids).stream()
                .map(imageMapper::toDomain)
                .toList();
    }

    @Override
    public List<PostImage> findByPostId(Long postId) {
        return imageJpaRepository.findAllByPostId(postId).stream()
                .map(imageMapper::toDomain)
                .toList();
    }

    @Override
    public List<PostImage> findExpired(ImageStatus status, LocalDateTime threshold, int limit) {
        return imageJpaRepository
                .findAllByStatusAndUpdatedAtBefore(status.name(), threshold, PageRequest.of(0, limit))
                .stream()
                .map(imageMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        imageJpaRepository.deleteAllByIdInBatch(ids);
    }
}
