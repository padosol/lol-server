package com.example.lolserver.gamedata.adapter.out.persistence.mapper;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;
import com.example.lolserver.gamedata.adapter.out.persistence.entity.VersionEntity;
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
        VersionEntity entity = createVersionEntity(1L, "26.16", "16.16.1", now);

        // when
        VersionReadModel result = mapper.entityToReadModel(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.versionId()).isEqualTo(1L);
        assertThat(result.versionValue()).isEqualTo("26.16");
        assertThat(result.patchVersionData()).isEqualTo("16.16.1");
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

    @DisplayName("데이터 버전이 비어 있는 엔티티는 patchVersionData 가 null 로 변환된다")
    @Test
    void entityToReadModel_nullPatchVersionData_returnsNullField() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        VersionEntity entity = createVersionEntity(2L, "26.1", null, now);

        // when
        VersionReadModel result = mapper.entityToReadModel(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.versionId()).isEqualTo(2L);
        assertThat(result.versionValue()).isEqualTo("26.1");
        assertThat(result.patchVersionData()).isNull();
    }

    private VersionEntity createVersionEntity(
            Long versionId, String versionValue, String patchVersionData, LocalDateTime createdAt
    ) throws Exception {
        java.lang.reflect.Constructor<VersionEntity> constructor = VersionEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        VersionEntity entity = constructor.newInstance();

        Field versionIdField = VersionEntity.class.getDeclaredField("versionId");
        versionIdField.setAccessible(true);
        versionIdField.set(entity, versionId);

        Field versionValueField = VersionEntity.class.getDeclaredField("versionValue");
        versionValueField.setAccessible(true);
        versionValueField.set(entity, versionValue);

        Field patchVersionDataField = VersionEntity.class.getDeclaredField("patchVersionData");
        patchVersionDataField.setAccessible(true);
        patchVersionDataField.set(entity, patchVersionData);

        Field createdAtField = VersionEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(entity, createdAt);

        return entity;
    }
}
