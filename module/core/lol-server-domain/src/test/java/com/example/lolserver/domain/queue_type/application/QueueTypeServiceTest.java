package com.example.lolserver.domain.queue_type.application;

import com.example.lolserver.domain.queue_type.application.port.out.QueueTypePersistencePort;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class QueueTypeServiceTest {

    @Mock
    private QueueTypePersistencePort queueTypePersistencePort;

    @InjectMocks
    private QueueTypeService queueTypeService;

    @DisplayName("큐 정보 조회 시 전체 큐 정보를 반환한다")
    @Test
    void getQueueInfo_데이터존재_전체큐정보반환() {
        // given
        List<QueueInfo> expected = List.of(
                new QueueInfo(420L, "솔로랭크", true),
                new QueueInfo(440L, "자유랭크", true),
                new QueueInfo(450L, "칼바람 나락", false)
        );
        given(queueTypePersistencePort.findAll()).willReturn(expected);

        // when
        List<QueueInfo> result = queueTypeService.getQueueInfo();

        // then
        assertThat(result).hasSize(3);
        assertThat(result).isEqualTo(expected);
        then(queueTypePersistencePort).should().findAll();
    }

    @DisplayName("큐 정보가 없는 경우 빈 리스트를 반환한다")
    @Test
    void getQueueInfo_데이터없음_빈리스트반환() {
        // given
        given(queueTypePersistencePort.findAll()).willReturn(Collections.emptyList());

        // when
        List<QueueInfo> result = queueTypeService.getQueueInfo();

        // then
        assertThat(result).isEmpty();
        then(queueTypePersistencePort).should().findAll();
    }

    @DisplayName("탭 표시 큐 조회 시 isTab=true인 큐만 반환한다")
    @Test
    void findAllByIsTabTrue_탭큐존재_필터링결과반환() {
        // given
        List<QueueInfo> expected = List.of(
                new QueueInfo(420L, "솔로랭크", true),
                new QueueInfo(440L, "자유랭크", true)
        );
        given(queueTypePersistencePort.findAllByIsTabTrue()).willReturn(expected);

        // when
        List<QueueInfo> result = queueTypeService.findAllByIsTabTrue();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(QueueInfo::isTab);
        then(queueTypePersistencePort).should().findAllByIsTabTrue();
    }

    @DisplayName("탭 표시 큐가 없는 경우 빈 리스트를 반환한다")
    @Test
    void findAllByIsTabTrue_탭큐없음_빈리스트반환() {
        // given
        given(queueTypePersistencePort.findAllByIsTabTrue()).willReturn(Collections.emptyList());

        // when
        List<QueueInfo> result = queueTypeService.findAllByIsTabTrue();

        // then
        assertThat(result).isEmpty();
        then(queueTypePersistencePort).should().findAllByIsTabTrue();
    }
}
