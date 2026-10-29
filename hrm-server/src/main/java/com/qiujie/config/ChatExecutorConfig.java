package com.qiujie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 智能问答线程池。
 * <p>
 * 与 {@code fileTaskExecutor}（批量文件导入/导出）隔离：一次 SSE 问答会占住一个线程直到
 * 流式输出结束，与文件任务共享池会让一次大批量导入把聊天挤进队列。
 * 队列满时直接拒绝（{@link ThreadPoolExecutor.AbortPolicy}），由调用方推 error 事件，
 * 而不是用 CallerRunsPolicy 把模型调用落回 Tomcat 请求线程。
 * </p>
 */
@Configuration
public class ChatExecutorConfig {

    @Bean("chatExecutor")
    public ThreadPoolTaskExecutor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("chat-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
