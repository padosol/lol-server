package com.example.lolserver.repository.version.mapper;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.repository.version.entity.VersionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VersionMapperTest {

    private final VersionMapper mapper = VersionMapper.INSTANCE;

    @DisplayName("VersionEntity를 VersionReadModel로 변환한다")
    @Test
    void entityToReadModel_validEntity_returnsReadModel() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        VersionEntity entity = createVersionEntity(1L, "14.24.1", now);

        // when
        VersionReadModel result = mapper.entityToReadModel(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.versionId()).isEqualTo(1L);
        assertThat(result.versionValue()).isEqualTo("14.24.1");
        assertThat(result.createdAt()).isEqualTo(now);
    }

    @DisplayName("null Entity는 null을 반환한다")
    @Test
    void entityToReadModel_nullEntity_returnsNull() {
        // when
        VersionReadModel result = mapper.entityToReadModel(null);

        // then
        assertThat(result).isNull();
    }

    @DisplayName("다양한 버전 형식을 올바르게 변환한다")
    @Test
    void entityToReadModel_variousVersionFormats_returnsCorrectReadModel() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        VersionEntity entity = createVersionEntity(2L, "15.1.1", now);

        // when
        VersionReadModel result = mapper.entityToReadModel(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.versionId()).isEqualTo(2L);
        assertThat(result.versionValue()).isEqualTo("15.1.1");
    }

    private VersionEntity createVersionEntity(Long versionId, String versionValue, LocalDateTime createdAt) throws Exception {
        java.lang.reflect.Constructor<VersionEntity> constructor = VersionEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        VersionEntity entity = constructor.newInstance();

        Field versionIdField = VersionEntity.class.getDeclaredField("versionId");
        versionIdField.setAccessible(true);
        versionIdField.set(entity, versionId);

        Field versionValueField = VersionEntity.class.getDeclaredField("versionValue");
        versionValueField.setAccessible(true);
        versionValueField.set(entity, versionValue);

        Field createdAtField = VersionEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(entity, createdAt);

        return entity;
    }
}
