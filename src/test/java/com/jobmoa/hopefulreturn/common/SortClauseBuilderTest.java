package com.jobmoa.hopefulreturn.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SortClauseBuilderTest {

    private static final Map<String, String> WHITELIST = Map.of(
            "name", "p.name COLLATE SQL_Latin1_General_CP1_CI_AS",
            "region", "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id");
    // 정렬 미지정 시 기본 본문에는 name(선택 가능 컬럼)이 포함돼 있다 — 정렬 지정 시 중복이 나면 안 된다.
    private static final String DEFAULT_ORDER_BY =
            "pr.region_id, r.region_id, p.name COLLATE SQL_Latin1_General_CP1_CI_AS, cp.id";
    private static final String STABLE_KEY = "cp.id";

    @Test
    @DisplayName("허용 키 + asc → 선택 컬럼 ASC 뒤에 PK(stableKey)만 tiebreaker 로 붙인다")
    void allowedKeyAsc() {
        String result = SortClauseBuilder.orderBy(WHITELIST, "name", "asc", DEFAULT_ORDER_BY, STABLE_KEY);

        assertThat(result).isEqualTo(
                "ORDER BY p.name COLLATE SQL_Latin1_General_CP1_CI_AS ASC, " + STABLE_KEY);
    }

    @Test
    @DisplayName("허용 키 + desc → 방향을 DESC 로 적용한다")
    void allowedKeyDesc() {
        String result = SortClauseBuilder.orderBy(WHITELIST, "name", "desc", DEFAULT_ORDER_BY, STABLE_KEY);

        assertThat(result).isEqualTo(
                "ORDER BY p.name COLLATE SQL_Latin1_General_CP1_CI_AS DESC, " + STABLE_KEY);
    }

    @Test
    @DisplayName("정렬 지정 시 선택 컬럼이 ORDER BY 에 한 번만 나온다(SQL Server 중복 컬럼 오류 방지)")
    void selectedColumnAppearsOnlyOnce() {
        // name 은 기본 본문에도 들어 있지만, 정렬 지정 시엔 stableKey(PK)만 붙으므로 중복되지 않는다.
        String result = SortClauseBuilder.orderBy(WHITELIST, "name", "asc", DEFAULT_ORDER_BY, STABLE_KEY);

        int occurrences = result.split("p\\.name", -1).length - 1;
        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    @DisplayName("복합 정렬(콤마 구분) 컬럼은 각 세그먼트에 동일 방향을 적용한다")
    void compositeColumnAppliesDirectionToEachSegment() {
        String result = SortClauseBuilder.orderBy(WHITELIST, "region", "desc", DEFAULT_ORDER_BY, STABLE_KEY);

        assertThat(result).isEqualTo(
                "ORDER BY CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END DESC, "
                        + "pr.region_id DESC, r.region_id DESC, " + STABLE_KEY);
    }

    @Test
    @DisplayName("화이트리스트에 없는 키 → 기본 정렬로 폴백(사용자 입력은 SQL 에 절대 들어가지 않음)")
    void unknownKeyFallsBack() {
        String result = SortClauseBuilder.orderBy(WHITELIST, "p.name; DROP TABLE x", "asc", DEFAULT_ORDER_BY, STABLE_KEY);

        assertThat(result).isEqualTo("ORDER BY " + DEFAULT_ORDER_BY);
    }

    @Test
    @DisplayName("sortBy 가 null → 기본 정렬로 폴백")
    void nullSortByFallsBack() {
        String result = SortClauseBuilder.orderBy(WHITELIST, null, "desc", DEFAULT_ORDER_BY, STABLE_KEY);

        assertThat(result).isEqualTo("ORDER BY " + DEFAULT_ORDER_BY);
    }

    @Test
    @DisplayName("sortOrder 가 desc 가 아닌 값(null·대문자·이상값)이면 ASC 로 간주한다")
    void nonDescOrderDefaultsToAsc() {
        assertThat(SortClauseBuilder.orderBy(WHITELIST, "name", null, DEFAULT_ORDER_BY, STABLE_KEY))
                .isEqualTo("ORDER BY p.name COLLATE SQL_Latin1_General_CP1_CI_AS ASC, " + STABLE_KEY);
        assertThat(SortClauseBuilder.orderBy(WHITELIST, "name", "DESC", DEFAULT_ORDER_BY, STABLE_KEY))
                .isEqualTo("ORDER BY p.name COLLATE SQL_Latin1_General_CP1_CI_AS DESC, " + STABLE_KEY);
        assertThat(SortClauseBuilder.orderBy(WHITELIST, "name", "garbage", DEFAULT_ORDER_BY, STABLE_KEY))
                .isEqualTo("ORDER BY p.name COLLATE SQL_Latin1_General_CP1_CI_AS ASC, " + STABLE_KEY);
    }
}
