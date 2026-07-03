package com.jobmoa.hopefulreturn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCrypt 인코더 임포트

/**
 * 빌드 게이트용 경량 테스트.
 * 컨텍스트 로드/통합 테스트는 추후 Testcontainers(MSSQL/Redis) 기반으로 추가한다.
 */
class HopefulReturnApplicationTests {

    @Test
    void sanityCheck() {
        // 1. 기존 기본 검증
        assertThat(1 + 1).isEqualTo(2);

        // 2. BCrypt 암호화 결과 콘솔 출력 (1234 인코딩)
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String bcryptResult = encoder.encode("1234");

        System.out.println("====================================================");
        System.out.println("★ 생성된 BCrypt 비밀번호: " + bcryptResult);
        System.out.println("====================================================");
    }
}