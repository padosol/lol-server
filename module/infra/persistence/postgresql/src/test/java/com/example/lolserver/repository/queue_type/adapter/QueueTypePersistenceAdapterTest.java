package com.example.lolserver.repository.queue_type.adapter;

import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.queue_type.QueueTypeRepository;
import com.example.lolserver.repository.queue_type.entity.QueueEntity;
import com.example.lolserver.repository.queue_type.mapper.QueueTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueueTypePersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private QueueTypeRepository queueTypeRepository;

    @Autowired
    private QueueTypeMapper queueTypeMapper;

    private QueueTypePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QueueTypePersistenceAdapter(queueTypeRepository, queueTypeMapper);
    }

    @DisplayName("전체 큐 타입을 조회하면 도메인 객체 리스트를 반환한다")
    @Test
    void findAll_multipleQueues_returnsDomainList() {
        // given
        QueueEntity queue1 = createQueueEntity(420L, "Ranked Solo/Duo", true);
        QueueEntity queue2 = createQueueEntity(440L, "Ranked Flex", true);
        QueueEntity queue3 = createQueueEntity(450L, "ARAM", false);
        queueTypeRepository.saveAll(List.of(queue1, queue2, queue3));

        // when
        List<QueueInfo> result = adapter.findAll();

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(QueueInfo::getQueueId)
                .containsExactlyInAnyOrder(420L, 440L, 450L);
    }

    @DisplayName("isTab이 true인 큐 타입만 조회하면 해당 도메인 객체만 반환한다")
    @Test
    void findAllByIsTabTrue_mixedQueues_returnsOnlyTabTrue() {
        // given
        QueueEntity tabQueue1 = createQueueEntity(420L, "Ranked Solo/Duo", true);
        QueueEntity tabQueue2 = createQueueEntity(440L, "Ranked Flex", true);
        QueueEntity nonTabQueue = createQueueEntity(450L, "ARAM", false);
        queueTypeRepository.saveAll(List.of(tabQueue1, tabQueue2, nonTabQueue));

        // when
        List<QueueInfo> result = adapter.findAllByIsTabTrue();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(QueueInfo::getQueueId)
                .containsExactlyInAnyOrder(420L, 440L);
        assertThat(result).allMatch(QueueInfo::isTab);
    }

    private QueueEntity createQueueEntity(Long queueId, String queueName, boolean isTab) {
        QueueEntity entity = new QueueEntity();
        ReflectionTestUtils.setField(entity, "queueId", queueId);
        ReflectionTestUtils.setField(entity, "queueName", queueName);
        ReflectionTestUtils.setField(entity, "isTab", isTab);
        return entity;
    }
}
