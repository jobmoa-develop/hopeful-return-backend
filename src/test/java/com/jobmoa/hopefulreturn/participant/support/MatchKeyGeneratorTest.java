package com.jobmoa.hopefulreturn.participant.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchKeyGeneratorTest {

    // ✅ PASS (2026-07-06)
    @Test
    @DisplayName("이니셜(로마자)_생년_전화뒤4 형식으로 생성한다 — 김철수/1978/010-5678-1234 → KCS_1978_1234")
    void generate_basic() {
        assertThat(MatchKeyGenerator.generate("김철수", 1978, "010-5678-1234"))
                .isEqualTo("KCS_1978_1234");
    }

    // ✅ PASS (2026-07-06)
    @Test
    @DisplayName("'ㅇ' 초성은 중성 로마자로 대체한다 — 이영희/1990/01011115678 → IYH_1990_5678")
    void generate_ieungLead() {
        assertThat(MatchKeyGenerator.generate("이영희", 1990, "01011115678"))
                .isEqualTo("IYH_1990_5678");
    }

    // ✅ PASS (2026-07-06)
    @Test
    @DisplayName("ㅂ/ㅈ 초성 로마자 매핑 — 박지성/2002/010-1234-9876 → PCS_2002_9876")
    void generate_variousLeads() {
        assertThat(MatchKeyGenerator.generate("박지성", 2002, "010-1234-9876"))
                .isEqualTo("PCS_2002_9876");
    }

    // ✅ PASS (2026-07-06)
    @Test
    @DisplayName("birthYear가 null이면 가운데를 빈 값으로 둔다")
    void generate_nullBirthYear() {
        assertThat(MatchKeyGenerator.generate("김철수", null, "010-5678-1234"))
                .isEqualTo("KCS__1234");
    }

    // ✅ PASS (2026-07-06)
    @Test
    @DisplayName("전화번호 숫자가 4자리 이하면 있는 그대로 사용한다")
    void generate_shortPhone() {
        assertThat(MatchKeyGenerator.generate("김철수", 1978, "12-3"))
                .isEqualTo("KCS_1978_123");
    }
}
