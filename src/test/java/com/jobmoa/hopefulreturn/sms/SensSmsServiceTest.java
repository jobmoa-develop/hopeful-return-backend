package com.jobmoa.hopefulreturn.sms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SENS SMS 서비스 단위 테스트. 실제 HTTP 호출 없이 수신번호 정규화 규칙만 검증한다.
 * (하이픈 포함 번호가 발송 시 SENS 에 거부되지 않도록 숫자만 남기는지 확인)
 */
class SensSmsServiceTest {

    // 실제 발송을 하지 않으므로 키는 더미로 주입한다.
    private final SensSmsService service = new SensSmsService("access", "secret", "serviceId", "15665011");

    @Test
    @DisplayName("하이픈 포함 번호는 숫자만 남긴다")
    void normalizePhone_stripsHyphen() {
        assertThat(service.normalizePhone("010-4301-7553")).isEqualTo("01043017553");
    }

    @Test
    @DisplayName("공백·점 등 숫자 이외 문자도 모두 제거한다")
    void normalizePhone_stripsSpacesAndDots() {
        assertThat(service.normalizePhone("010 1234 5678")).isEqualTo("01012345678");
        assertThat(service.normalizePhone("010.1234.5678")).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("이미 정규화된 번호는 그대로 둔다")
    void normalizePhone_keepsAlreadyNormalized() {
        assertThat(service.normalizePhone("01043017553")).isEqualTo("01043017553");
    }

    @Test
    @DisplayName("null 은 빈 문자열로 처리한다")
    void normalizePhone_null() {
        assertThat(service.normalizePhone(null)).isEmpty();
    }
}
