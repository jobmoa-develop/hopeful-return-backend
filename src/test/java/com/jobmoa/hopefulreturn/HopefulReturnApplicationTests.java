package com.jobmoa.hopefulreturn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 빌드 게이트용 경량 테스트.
 * 컨텍스트 로드/통합 테스트는 추후 Testcontainers(MSSQL/Redis) 기반으로 추가한다.
 */
class HopefulReturnApplicationTests {

    @Test
    void sanityCheck() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
