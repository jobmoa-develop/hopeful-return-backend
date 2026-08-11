package com.jobmoa.hopefulreturn.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간 소스(Clock) 빈. 시각 의존 로직(QR 입·퇴실 상태 머신 등)이 {@link Clock} 을 주입받아
 * 테스트에서 고정 시계로 대체할 수 있도록 한다.
 *
 * <p>서버 JVM 기본 타임존(컨테이너 배포 시 흔히 UTC)에 흔들리지 않도록 한국 시간(Asia/Seoul)을
 * 명시한다. 교육 일차(dayNo) 판정과 입·퇴실 시각이 KST 기준이어야 하기 때문이다.
 */
@Configuration
public class ClockConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
