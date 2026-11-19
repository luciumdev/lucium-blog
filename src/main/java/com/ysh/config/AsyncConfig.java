package com.ysh.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 설정
 * 1. esIndexExecutor 별도 풀
 * 2. ThreadPoolTaskExecutor 채택 — Virtual Thread 대신
 * 3. AsyncUncaughtExceptionHandler 명시
 * 4. corePoolSize=2, max=8, queue=100 -> 트래픽 늘면 조정할 것 (과연)
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean("esIndexExecutor")
    public Executor esIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("es-index-");
        executor.setRejectedExecutionHandler((task, ex) -> {
            log.error("esIndexExecutor 큐 포화 — ES 색인 작업 거부됨. 복구 필요함");
        });
        executor.initialize();
        return executor;
    }

    /**
     * @Async 기본 Executor — 외부의 비동기 작업용
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-default-");
        executor.initialize();
        return executor;
    }

    /**
     * @Async 미처리 예외 핸들러
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params)-> log.error("@Async 메서드에서 미처리 예외 발생 — method={}, params={}", method.getName(), params, throwable);
    }

    /**
     * 백필 전용 executor
     * @return
     */
    @Bean("reindexExecutor")
    public Executor reindexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("reindex-");
        executor.setRejectedExecutionHandler((task, ex) -> {
            log.warn("reindexExecutor 거부 - 이미 백필 진행 중임");
        });
        executor.initialize();
        return executor;
    }
}
