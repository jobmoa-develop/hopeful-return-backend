package com.jobmoa.hopefulreturn.participant.repository;

import com.jobmoa.hopefulreturn.common.SortClauseBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * {@link ParticipantRepositoryCustom} 구현 — 참여자 목록의 동적 정렬을 처리한다.
 *
 * <p>기존 네이티브 @Query 의 CTE·WHERE 블록을 그대로 옮기고(회귀 최소화), ORDER BY 만
 * {@link SortClauseBuilder} 로 화이트리스트 기반 조립한다. sortBy 가 null/미허용이면 기존과
 * 동일한 기본 정렬(지역 표시순 → 이름)로 폴백하므로 정렬 미지정 시 동작이 바뀌지 않는다.
 */
public class ParticipantRepositoryImpl implements ParticipantRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    // 정렬 화이트리스트 — 키(FE 전달값) → 하드코딩된 컬럼식. 원문 sortBy 는 SQL 에 삽입하지 않는다.
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "name", "p.name COLLATE SQL_Latin1_General_CP1_CI_AS",
            "phone", "p.phone",
            "region", "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id",
            "registerDate", "l.created_at");

    // 정렬 미지정 시 사용할 기존 기본 정렬(지역 표시순 → 이름) — 기존 @Query ORDER BY 본문과 동일.
    private static final String DEFAULT_ORDER_BY =
            "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id, "
                    + "p.name COLLATE SQL_Latin1_General_CP1_CI_AS";
    // 정렬 지정 시 tiebreaker 로 붙일 고유 안정키(PK) — 선택 컬럼 중복을 피하면서 결정적 순서 보장.
    private static final String STABLE_KEY = "p.participant_id";

    // 참여자별 "최신 수강건"(course_participant_id 최댓값) CTE — ID·COUNT 쿼리에 공통.
    private static final String CTE = """
            WITH latest AS (
                SELECT cp.*, ROW_NUMBER() OVER (
                         PARTITION BY cp.participant_id
                         ORDER BY cp.course_participant_id DESC) AS rn
                FROM course_participant cp
            )
            """;

    // WHERE 조건절 — p/c/l 만 참조하므로 ID 쿼리(지역 조인 포함)·COUNT 쿼리(지역 조인 없음) 양쪽에 재사용.
    private static final String WHERE_CONDITIONS = """
            WHERE (:name IS NULL OR p.name LIKE '%' + :name + '%')
              AND (:phone IS NULL OR p.phone LIKE '%' + :phone + '%')
              AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
              AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
              AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
              AND (:registerDateFrom IS NULL OR CAST(l.created_at AS DATE) >= :registerDateFrom)
              AND (:registerDateTo IS NULL OR CAST(l.created_at AS DATE) <= :registerDateTo)
              AND (:scopeOff = 1 OR p.participant_id IN (:allowedIds))
            """;

    private static final String ID_FROM = """
            SELECT p.participant_id
            FROM participant p
            LEFT JOIN latest l ON l.participant_id = p.participant_id AND l.rn = 1
            LEFT JOIN course c ON c.course_id = l.course_id
            LEFT JOIN region r ON r.region_id = c.region_id
            LEFT JOIN region pr ON pr.region_id = r.parent_region_id
            """;

    private static final String COUNT_FROM = """
            SELECT COUNT(*)
            FROM participant p
            LEFT JOIN latest l ON l.participant_id = p.participant_id AND l.rn = 1
            LEFT JOIN course c ON c.course_id = l.course_id
            """;

    @Override
    public Page<Long> findFilteredParticipantIdsSorted(
            String name,
            String phone,
            int hasRegion,
            List<Long> regionIds,
            Integer courseNumber,
            Integer localCourseNumber,
            LocalDate registerDateFrom,
            LocalDate registerDateTo,
            int scopeOff,
            List<Long> allowedIds,
            String sortBy,
            String sortOrder,
            Pageable pageable) {

        String orderBy = SortClauseBuilder.orderBy(SORT_WHITELIST, sortBy, sortOrder, DEFAULT_ORDER_BY, STABLE_KEY);

        String idSql = CTE + ID_FROM + WHERE_CONDITIONS + orderBy;
        Query idQuery = em.createNativeQuery(idSql);
        bindFilters(idQuery, name, phone, hasRegion, regionIds, courseNumber, localCourseNumber,
                registerDateFrom, registerDateTo, scopeOff, allowedIds);
        // Pageable.unpaged() 는 getOffset()/getPageSize() 가 예외를 던지므로, 페이징된 경우에만
        // offset/limit 을 적용한다(unpaged 는 전건 조회).
        if (pageable.isPaged()) {
            idQuery.setFirstResult((int) pageable.getOffset());
            idQuery.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Object> rows = idQuery.getResultList();
        List<Long> ids = rows.stream().map(value -> ((Number) value).longValue()).toList();

        String countSql = CTE + COUNT_FROM + WHERE_CONDITIONS;
        Query countQuery = em.createNativeQuery(countSql);
        bindFilters(countQuery, name, phone, hasRegion, regionIds, courseNumber, localCourseNumber,
                registerDateFrom, registerDateTo, scopeOff, allowedIds);

        return PageableExecutionUtils.getPage(
                ids, pageable, () -> ((Number) countQuery.getSingleResult()).longValue());
    }

    // 기존 @Query 의 파라미터 목록과 1:1 대응 — 누락 시 런타임 오류이므로 정확히 유지한다.
    private void bindFilters(
            Query query,
            String name,
            String phone,
            int hasRegion,
            List<Long> regionIds,
            Integer courseNumber,
            Integer localCourseNumber,
            LocalDate registerDateFrom,
            LocalDate registerDateTo,
            int scopeOff,
            List<Long> allowedIds) {
        query.setParameter("name", name);
        query.setParameter("phone", phone);
        query.setParameter("hasRegion", hasRegion);
        query.setParameter("regionIds", regionIds);
        query.setParameter("courseNumber", courseNumber);
        query.setParameter("localCourseNumber", localCourseNumber);
        query.setParameter("registerDateFrom", registerDateFrom);
        query.setParameter("registerDateTo", registerDateTo);
        query.setParameter("scopeOff", scopeOff);
        query.setParameter("allowedIds", allowedIds);
    }
}
