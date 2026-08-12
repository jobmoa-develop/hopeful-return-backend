package com.jobmoa.hopefulreturn.common;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 목록 조회의 서버사이드 정렬(sortBy/sortOrder)을 안전하게 ORDER BY 절로 조립하는 유틸.
 *
 * <p>네이티브 쿼리에 사용자 입력(sortBy)을 절대 직접 넣지 않는다. 도메인별로 정의한
 * {@code Map<허용키, 하드코딩된 컬럼식>} 의 <b>값(하드코딩된 SQL 식)만</b> ORDER BY 에 삽입해
 * SQL injection 을 원천 차단한다. sortOrder 도 {@code ASC}/{@code DESC} 두 상수로만 환원한다.
 *
 * <p>선택 컬럼을 1순위로 두고, 뒤에 <b>고유 안정키(stableKey, 보통 PK)</b>만 tiebreaker 로 붙여
 * 결정적 순서를 보장한다. sortBy 가 null 이거나 화이트리스트에 없으면 도메인 기본 정렬
 * ({@code defaultOrderBy})로 폴백한다.
 *
 * <p><b>중복 방지</b>: 선택 컬럼이 기본 정렬 본문에도 들어 있으면 ORDER BY 에 같은 컬럼이 두 번
 * 나와 SQL Server 가 거부한다("Columns in the order by list must be unique"). 그래서 정렬이
 * 지정된 경우엔 전체 기본 본문이 아니라 PK 단독을 tiebreaker 로 쓴다.
 *
 * <p><b>제약</b>: 화이트리스트의 컬럼식은 콤마를 "정렬 세그먼트 구분자"로만 사용해야 한다
 * (복합 정렬 예: 지역 = {@code "pr.region_id, r.region_id"}). 각 세그먼트에 동일한 방향을
 * 적용하므로, 콤마를 포함하는 함수 호출식(예: {@code CONCAT(a, b)})은 컬럼식으로 쓸 수 없다.
 */
public final class SortClauseBuilder {

    private SortClauseBuilder() {
    }

    /**
     * @param whitelist      허용키 → 하드코딩된 컬럼식(예: {@code "p.name COLLATE SQL_Latin1_General_CP1_CI_AS"})
     * @param sortBy         요청된 정렬 키(사용자 입력, 화이트리스트 매칭 실패 시 무시)
     * @param sortOrder      {@code asc}/{@code desc}(대소문자 무관, desc 외에는 asc 로 간주)
     * @param defaultOrderBy 정렬 미지정 시 사용할 기존 기본 ORDER BY 본문(예: 지역→이름→PK)
     * @param stableKey      정렬 지정 시 tiebreaker 로 붙일 고유 안정키(보통 PK, 예: {@code "cp.course_participant_id"})
     * @return {@code "ORDER BY <컬럼식> <ASC|DESC>, <stableKey>"} 또는 폴백 {@code "ORDER BY <defaultOrderBy>"}
     */
    public static String orderBy(
            Map<String, String> whitelist, String sortBy, String sortOrder, String defaultOrderBy, String stableKey) {
        String column = (whitelist == null || sortBy == null) ? null : whitelist.get(sortBy);
        if (column == null) {
            return "ORDER BY " + defaultOrderBy;
        }
        String direction = "desc".equalsIgnoreCase(sortOrder == null ? null : sortOrder.trim()) ? "DESC" : "ASC";
        // 매핑된 컬럼식이 콤마로 구분된 복합 정렬(예: 지역 = 부모ID, 자식ID)일 수 있으므로,
        // 각 세그먼트에 동일 방향을 적용해 복합 정렬도 일관되게 뒤집는다.
        String withDirection = Arrays.stream(column.split(","))
                .map(String::trim)
                .map(segment -> segment + " " + direction)
                .collect(Collectors.joining(", "));
        return "ORDER BY " + withDirection + ", " + stableKey;
    }
}
