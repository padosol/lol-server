package com.example.lolserver.repository.queue_type.mapper;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.repository.queue_type.entity.QueueEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class QueueTypeMapperTest {

    private final QueueTypeMapper mapper = QueueTypeMapper.INSTANCE;

    @DisplayName("QueueEntity를 QueueInfo 도메인으로 변환한다")
    @Test
    void entityToDomain_validEntity_returnsQueueInfo() throws Exception {
        // given
        QueueEntity entity = createQueueEntity(420L, "5v5 Ranked Solo games", true);

        // when
        QueueInfo result = mapper.entityToDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getQueueId()).isEqualTo(420L);
        assertThat(result.getQueueName()).isEqualTo("5v5 Ranked Solo games");
        assertThat(result.isTab()).isTrue();
    }

    @DisplayName("isTab이 false인 QueueEntity를 올바르게 변환한다")
    @Test
    void entityToDomain_isTabFalse_returnsQueueInfoWithFalseTab() throws Exception {
        // given
        QueueEntity entity = createQueueEntity(450L, "5v5 ARAM games", false);

        // when
        QueueInfo result = mapper.entityToDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getQueueId()).isEqualTo(450L);
        assertThat(result.getQueueName()).isEqualTo("5v5 ARAM games");
        assertThat(result.isTab()).isFalse();
    }

    @DisplayName("Flex 랭크 큐를 올바르게 변환한다")
    @Test
    void entityToDomain_flexRankedQueue_returnsFlexQueueInfo() throws Exception {
        // given
        QueueEntity entity = createQueueEntity(440L, "5v5 Ranked Flex games", true);

        // when
        QueueInfo result = mapper.entityToDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getQueueId()).isEqualTo(440L);
        assertThat(result.getQueueName()).isEqualTo("5v5 Ranked Flex games");
        assertThat(result.isTab()).isTrue();
    }

    @DisplayName("null queueName이 있는 Entity를 변환한다")
    @Test
    void entityToDomain_nullQueueName_returnsQueueInfoWithNullName() throws Exception {
        // given
        QueueEntity entity = createQueueEntity(0L, null, false);

        // when
        QueueInfo result = mapper.entityToDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getQueueId()).isEqualTo(0L);
        assertThat(result.getQueueName()).isNull();
        assertThat(result.isTab()).isFalse();
    }

    /**
     * QueueEntity는 기본 생성자가 private이므로 리플렉션을 사용해 생성
     */
    private QueueEntity createQueueEntity(Long queueId, String queueName, boolean isTab) throws Exception {
        QueueEntity entity = QueueEntity.class.getDeclaredConstructor().newInstance();

        Field queueIdField = QueueEntity.class.getDeclaredField("queueId");
        queueIdField.setAccessible(true);
        queueIdField.set(entity, queueId);

        Field queueNameField = QueueEntity.class.getDeclaredField("queueName");
        queueNameField.setAccessible(true);
        queueNameField.set(entity, queueName);

        Field isTabField = QueueEntity.class.getDeclaredField("isTab");
        isTabField.setAccessible(true);
        isTabField.set(entity, isTab);

        return entity;
    }
}
