package com.example.lolserver.repository.version.adapter;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.version.VersionJpaRepository;
import com.example.lolserver.repository.version.entity.VersionEntity;
import com.example.lolserver.repository.version.mapper.VersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VersionPersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private VersionJpaRepository versionJpaRepository;

    @Autowired
    private VersionMapper versionMapper;

    private VersionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        versionJpaRepository.deleteAll();
        adapter = new VersionPersistenceAdapter(versionJpaRepository, versionMapper);
    }

    @DisplayName("최신 버전을 조회하면 가장 최근 버전을 반환한다")
    @Test
    void findLatestVersion_multipleVersions_returnsLatest() {
        // given
        VersionEntity v1 = createVersionEntity("14.22.1");
        VersionEntity v2 = createVersionEntity("14.23.1");
        VersionEntity v3 = createVersionEntity("14.24.1");
        versionJpaRepository.saveAll(List.of(v1, v2, v3));

        // when
        Optional<VersionReadModel> result = adapter.findLatestVersion();

        // then
        assertThat(result).isPresent();
        assertThat(result.get().versionValue()).isEqualTo("14.24.1");
    }

    @DisplayName("버전이 없으면 빈 Optional을 반환한다")
    @Test
    void findLatestVersion_noVersions_returnsEmpty() {
        // when
        Optional<VersionReadModel> result = adapter.findLatestVersion();

        // then
        assertThat(result).isEmpty();
    }

    @DisplayName("전체 버전을 조회하면 최신순으로 정렬된 목록을 반환한다")
    @Test
    void findAllVersions_multipleVersions_returnsOrderedList() {
        // given
        VersionEntity v1 = createVersionEntity("14.22.1");
        VersionEntity v2 = createVersionEntity("14.23.1");
        VersionEntity v3 = createVersionEntity("14.24.1");
        versionJpaRepository.saveAll(List.of(v1, v2, v3));

        // when
        List<VersionReadModel> result = adapter.findAllVersions();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).versionValue()).isEqualTo("14.24.1");
        assertThat(result.get(1).versionValue()).isEqualTo("14.23.1");
        assertThat(result.get(2).versionValue()).isEqualTo("14.22.1");
    }

    @DisplayName("ID로 버전을 조회하면 해당 버전을 반환한다")
    @Test
    void findById_existingVersion_returnsVersion() {
        // given
        VersionEntity v1 = createVersionEntity("14.24.1");
        VersionEntity saved = versionJpaRepository.save(v1);

        // when
        Optional<VersionReadModel> result = adapter.findById(saved.getVersionId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().versionValue()).isEqualTo("14.24.1");
    }

    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    @Test
    void findById_nonExistingVersion_returnsEmpty() {
        // when
        Optional<VersionReadModel> result = adapter.findById(999L);

        // then
        assertThat(result).isEmpty();
    }

    private VersionEntity createVersionEntity(String versionValue) {
        return new VersionEntity(versionValue);
    }
}
