package com.example.lolserver.repository.version.adapter;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.domain.version.application.port.out.VersionPersistencePort;
import com.example.lolserver.repository.version.VersionJpaRepository;
import com.example.lolserver.repository.version.mapper.VersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VersionPersistenceAdapter implements VersionPersistencePort {

    private final VersionJpaRepository versionJpaRepository;
    private final VersionMapper versionMapper;

    @Override
    public Optional<VersionReadModel> findLatestVersion() {
        return versionJpaRepository.findLatestVersion()
                .map(versionMapper::entityToReadModel);
    }

    @Override
    public List<VersionReadModel> findAllVersions() {
        return versionJpaRepository.findAllOrderByVersionIdDesc()
                .stream()
                .map(versionMapper::entityToReadModel)
                .toList();
    }

    @Override
    public Optional<VersionReadModel> findById(Long versionId) {
        return versionJpaRepository.findById(versionId)
                .map(versionMapper::entityToReadModel);
    }
}
