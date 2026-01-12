package com.example.lolserver.repository.queue_type;

import com.example.lolserver.repository.config.RepositoryTestBase;
import com.example.lolserver.repository.queue_type.entity.QueueEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class QueueTypeRepositoryTest extends RepositoryTestBase {

    @Autowired
    private QueueTypeRepository queueTypeRepository;

    @DisplayName("큐 정보를 저장하고 ID로 조회한다")
    @Test
    void save_validQueue_findById() {
        // given
        QueueEntity queue = new QueueEntity();
        ReflectionTestUtils.setField(queue, "queueId", 420L);
        ReflectionTestUtils.setField(queue, "queueName", "Ranked Solo/Duo");
        ReflectionTestUtils.setField(queue, "isTab", true);

        // when
        QueueEntity saved = queueTypeRepository.save(queue);
        Optional<QueueEntity> found = queueTypeRepository.findById(saved.getQueueId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getQueueId()).isEqualTo(420L);
        assertThat(found.get().getQueueName()).isEqualTo("Ranked Solo/Duo");
    }

    @DisplayName("여러 큐를 저장하고 전체 목록을 조회한다")
    @Test
    void findAll_multipleQueues_returnsAll() {
        // given
        QueueEntity queue1 = new QueueEntity();
        ReflectionTestUtils.setField(queue1, "queueId", 420L);
        ReflectionTestUtils.setField(queue1, "queueName", "Ranked Solo/Duo");
        ReflectionTestUtils.setField(queue1, "isTab", true);

        QueueEntity queue2 = new QueueEntity();
        ReflectionTestUtils.setField(queue2, "queueId", 440L);
        ReflectionTestUtils.setField(queue2, "queueName", "Ranked Flex");
        ReflectionTestUtils.setField(queue2, "isTab", false);

        queueTypeRepository.saveAll(List.of(queue1, queue2));

        // when
        List<QueueEntity> result = queueTypeRepository.findAll();

        // then
        assertThat(result).hasSize(2);
    }

    @DisplayName("isTab이 true인 큐만 조회한다")
    @Test
    void findAllByIsTab_tabTrue_returnsOnlyTabTrue() {
        // given
        QueueEntity tabQueue = new QueueEntity();
        ReflectionTestUtils.setField(tabQueue, "queueId", 420L);
        ReflectionTestUtils.setField(tabQueue, "queueName", "Ranked Solo/Duo");
        ReflectionTestUtils.setField(tabQueue, "isTab", true);

        QueueEntity nonTabQueue = new QueueEntity();
        ReflectionTestUtils.setField(nonTabQueue, "queueId", 450L);
        ReflectionTestUtils.setField(nonTabQueue, "queueName", "ARAM");
        ReflectionTestUtils.setField(nonTabQueue, "isTab", false);

        queueTypeRepository.saveAll(List.of(tabQueue, nonTabQueue));

        // when
        List<QueueEntity> result = queueTypeRepository.findAllByIsTab(true);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQueueName()).isEqualTo("Ranked Solo/Duo");
        assertThat(result.get(0).isTab()).isTrue();
    }
}
