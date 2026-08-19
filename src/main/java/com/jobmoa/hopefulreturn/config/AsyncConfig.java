package com.jobmoa.hopefulreturn.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 실행 설정. 현재는 인력 근무불가 메일 알림 발송을 요청 스레드와 분리해 처리하는 데 쓴다.
 * 알림은 원 요청(스케줄 변경)의 성공/응답에 영향을 주면 안 되므로 별도 스레드풀로 격리한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 알림 발송 전용 executor. 발송은 소량·저빈도라 작은 풀로 충분하다. */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notify-");
        executor.initialize();
        return executor;
    }
}
