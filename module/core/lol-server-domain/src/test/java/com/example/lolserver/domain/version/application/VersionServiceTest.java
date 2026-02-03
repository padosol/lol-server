package com.example.lolserver.domain.version.application;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.domain.version.application.port.out.VersionPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@DisplayName("VersionService 테스트")
@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private VersionFinder versionFinder;

    @Mock
    private VersionPersistencePort versionPersistencePort;

    @InjectMocks
    private VersionService versionService;

    @DisplayName("최신 버전 조회 - Finder를 통해 반환한다")
    @Test
    void getLatestVersion_Finder통해반환() {
        // given
        VersionReadModel latestVersion = createVersionReadModel(1L, "14.24.1");
        given(versionFinder.findLatestVersion()).willReturn(latestVersion);

        // when
        VersionReadModel result = versionService.getLatestVersion();

        // then
        assertThat(result).isNotNull();
        assertThat(result.versionId()).isEqualTo(1L);
        assertThat(result.versionValue()).isEqualTo("14.24.1");
        then(versionFinder).should().findLatestVersion();
    }

    @DisplayName("최신 버전 조회 - 데이터 없으면 null 반환")
    @Test
    void getLatestVersion_데이터없음_null반환() {
        // given
        given(versionFinder.findLatestVersion()).willReturn(null);

        // when
        VersionReadModel result = versionService.getLatestVersion();

        // then
        assertThat(result).isNull();
        then(versionFinder).should().findLatestVersion();
    }

    @DisplayName("전체 버전 조회 - 목록 반환")
    @Test
    void getAllVersions_목록반환() {
        // given
        List<VersionReadModel> versions = List.of(
                createVersionReadModel(3L, "14.24.1"),
                createVersionReadModel(2L, "14.23.1"),
                createVersionReadModel(1L, "14.22.1")
        );
        given(versionPersistencePort.findAllVersions()).willReturn(versions);

        // when
        List<VersionReadModel> result = versionService.getAllVersions();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).versionValue()).isEqualTo("14.24.1");
        assertThat(result.get(2).versionValue()).isEqualTo("14.22.1");
        then(versionPersistencePort).should().findAllVersions();
    }

    @DisplayName("ID로 버전 조회 - 해당 버전 반환")
    @Test
    void getVersionById_해당버전반환() {
        // given
        Long versionId = 1L;
        VersionReadModel version = createVersionReadModel(versionId, "14.24.1");
        given(versionPersistencePort.findById(versionId)).willReturn(Optional.of(version));

        // when
        VersionReadModel result = versionService.getVersionById(versionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.versionId()).isEqualTo(versionId);
        assertThat(result.versionValue()).isEqualTo("14.24.1");
        then(versionPersistencePort).should().findById(versionId);
    }

    @DisplayName("ID로 버전 조회 - 없으면 null 반환")
    @Test
    void getVersionById_없으면_null반환() {
        // given
        Long versionId = 999L;
        given(versionPersistencePort.findById(versionId)).willReturn(Optional.empty());

        // when
        VersionReadModel result = versionService.getVersionById(versionId);

        // then
        assertThat(result).isNull();
        then(versionPersistencePort).should().findById(versionId);
    }

    private VersionReadModel createVersionReadModel(Long id, String versionValue) {
        return new VersionReadModel(id, versionValue, LocalDateTime.now());
    }
}
