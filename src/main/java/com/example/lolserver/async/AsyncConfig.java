//package com.example.lolserver.async;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.scheduling.annotation.AsyncConfigurer;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//
//@EnableAsync
//@Configuration
//public class AsyncConfig implements AsyncConfigurer {
//
//    @Override
//    @Bean(name = "taskExecutor")
//    public TaskExecutor getAsyncExecutor() {
//
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setThreadNamePrefix("async-thread-");
//        executor.setCorePoolSize(10);
//        executor.setMaxPoolSize(30);
//        executor.setQueueCapacity(100);
//
//        executor.initialize();
//
//        return executor;
//    }
//
//
//}
