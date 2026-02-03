package com.example.lolserver.domain.version.application;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.domain.version.application.port.out.VersionCachePort;
import com.example.lolserver.domain.version.application.port.out.VersionPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("VersionFinder 테스트")
@ExtendWith(MockitoExtension.class)
class VersionFinderTest {

    @Mock
    private VersionCachePort versionCachePort;

    @Mock
    private VersionPersistencePort versionPersistencePort;

    private VersionFinder versionFinder;

    @BeforeEach
    void setUp() {
        versionFinder = new VersionFinder(versionCachePort, versionPersistencePort);
    }

    @DisplayName("캐시에 데이터가 있으면 캐시 결과를 반환하고 DB는 호출하지 않는다")
    @Test
    void findLatestVersion_캐시에데이터있음_캐시결과반환() {
        // given
        VersionReadModel cachedResult = createVersionReadModel(1L, "14.24.1");

        given(versionCachePort.findLatestVersion())
                .willReturn(cachedResult);

        // when
        VersionReadModel result = versionFinder.findLatestVersion();

        // then
        assertThat(result).isEqualTo(cachedResult);
        assertThat(result.versionValue()).isEqualTo("14.24.1");
        then(versionCachePort).should().findLatestVersion();
        then(versionPersistencePort).should(never()).findLatestVersion();
    }

    @DisplayName("캐시가 null이면 DB에서 조회 후 캐시에 저장한다")
    @Test
    void findLatestVersion_캐시null_DB조회후캐시저장() {
        // given
        VersionReadModel dbResult = createVersionReadModel(1L, "14.24.1");

        given(versionCachePort.findLatestVersion())
                .willReturn(null);
        given(versionPersistencePort.findLatestVersion())
                .willReturn(Optional.of(dbResult));

        // when
        VersionReadModel result = versionFinder.findLatestVersion();

        // then
        assertThat(result).isEqualTo(dbResult);
        assertThat(result.versionValue()).isEqualTo("14.24.1");
        then(versionCachePort).should().findLatestVersion();
        then(versionPersistencePort).should().findLatestVersion();
        then(versionCachePort).should().saveLatestVersion(dbResult);
    }

    @DisplayName("캐시와 DB 모두 없으면 null을 반환한다")
    @Test
    void findLatestVersion_둘다없음_null반환() {
        // given
        given(versionCachePort.findLatestVersion())
                .willReturn(null);
        given(versionPersistencePort.findLatestVersion())
                .willReturn(Optional.empty());

        // when
        VersionReadModel result = versionFinder.findLatestVersion();

        // then
        assertThat(result).isNull();
        then(versionCachePort).should().findLatestVersion();
        then(versionPersistencePort).should().findLatestVersion();
        then(versionCachePort).should(never()).saveLatestVersion(any());
    }

    private VersionReadModel createVersionReadModel(Long id, String versionValue) {
        return new VersionReadModel(id, versionValue, LocalDateTime.now());
    }
}
