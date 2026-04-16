package com.example.lolserver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecutorConfig 테스트")
class ExecutorConfigTest {

    private ExecutorConfig executorConfig;

    @BeforeEach
    void setUp() {
        executorConfig = new ExecutorConfig();
    }

    // ========== TaskScheduler 테스트 ==========

    @DisplayName("TaskScheduler 빈 생성 시 ThreadPoolTaskScheduler를 반환한다")
    @Test
    void taskScheduler_빈생성시_ThreadPoolTaskScheduler반환() {
        // when
        TaskScheduler scheduler = executorConfig.taskScheduler();

        // then
        assertThat(scheduler).isInstanceOf(ThreadPoolTaskScheduler.class);
    }

    @DisplayName("TaskScheduler의 poolSize가 10으로 설정된다")
    @Test
    void taskScheduler_poolSize_10설정확인() {
        // when
        TaskScheduler scheduler = executorConfig.taskScheduler();

        // then
        ThreadPoolTaskScheduler threadPoolScheduler = (ThreadPoolTaskScheduler) scheduler;
        assertThat(threadPoolScheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(10);
    }

    // ========== TaskExecutor 테스트 ==========

    @DisplayName("TaskExecutor 빈 생성 시 ThreadPoolTaskExecutor를 반환한다")
    @Test
    void taskExecutor_빈생성시_ThreadPoolTaskExecutor반환() {
        // when
        Executor executor = executorConfig.taskExecutor();

        // then
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @DisplayName("TaskExecutor의 corePoolSize가 10으로 설정된다")
    @Test
    void taskExecutor_corePoolSize_10설정확인() {
        // when
        Executor executor = executorConfig.taskExecutor();

        // then
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(10);
    }

    @DisplayName("TaskExecutor의 maxPoolSize가 20으로 설정된다")
    @Test
    void taskExecutor_maxPoolSize_20설정확인() {
        // when
        Executor executor = executorConfig.taskExecutor();

        // then
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(20);
    }

    @DisplayName("TaskExecutor의 queueCapacity가 30으로 설정된다")
    @Test
    void taskExecutor_queueCapacity_30설정확인() {
        // when
        Executor executor = executorConfig.taskExecutor();

        // then
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(30);
    }
}
