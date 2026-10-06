package com.example.coffee.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String OUTBOX_PUBLISH_EXECUTOR = "outboxPublishExecutor";

    @Bean(name = OUTBOX_PUBLISH_EXECUTOR)
    public ThreadPoolTaskExecutor outboxPublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("outbox-publish-");
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("즉시 발행 대기열이 가득 차 작업을 건너뜁니다. 재발행 Relay가 처리합니다."));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}