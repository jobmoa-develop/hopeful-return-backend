package com.jobmoa.hopefulreturn.courseparticipant.repository;

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
 * {@link CourseParticipantRepositoryCustom} 구현 — 상담/수강생 목록의 동적 정렬을 처리한다.
 *
 * <p>기존 네이티브 @Query 의 WHERE 블록을 그대로 옮기고(회귀 최소화), ORDER BY 만
 * {@link SortClauseBuilder} 로 화이트리스트 기반 조립한다. sortBy 가 null/미허용이면 기존과
 * 동일한 기본 정렬(지역 표시순 → 이름 → id)로 폴백하므로 정렬 미지정 시 동작이 바뀌지 않는다.
 */
public class CourseParticipantRepositoryImpl implements CourseParticipantRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    // 정렬 화이트리스트 — 키(FE 전달값) → 하드코딩된 컬럼식. 원문 sortBy 는 SQL 에 삽입하지 않는다.
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "participantName", "p.name COLLATE SQL_Latin1_General_CP1_CI_AS",
            "phone", "p.phone",
            "status", "cp.status",
            "region", "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id",
            "courseNumber", "c.course_number",
            "localCourseNumber", "c.local_course_number",
            "registerDate", "cp.created_at",
            "completionDate", "cp.completion_date");

    // 정렬 미지정 시 사용할 기존 기본 정렬 — 기존 @Query ORDER BY 본문과 동일(지역→이름→PK).
    private static final String DEFAULT_ORDER_BY =
            "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id, "
                    + "p.name COLLATE SQL_Latin1_General_CP1_CI_AS, cp.course_participant_id";
    // 정렬 지정 시 tiebreaker 로 붙일 고유 안정키(PK) — 선택 컬럼 중복을 피하면서 결정적 순서 보장.
    private static final String STABLE_KEY = "cp.course_participant_id";

    // WHERE 조건절 — cp/p/c 만 참조하므로 ID 쿼리(지역 조인 포함)·COUNT 쿼리(지역 조인 없음) 양쪽에 재사용.
    private static final String WHERE_CONDITIONS = """
            WHERE (:courseId IS NULL OR cp.course_id = :courseId)
              AND (:status IS NULL OR cp.status = :status)
              AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
              AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
              AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
              AND (:keyword IS NULL OR p.name LIKE '%' + :keyword + '%' OR p.phone LIKE '%' + :keyword + '%')
              AND (:registerDateFrom IS NULL OR CAST(cp.created_at AS DATE) >= :registerDateFrom)
              AND (:registerDateTo IS NULL OR CAST(cp.created_at AS DATE) <= :registerDateTo)
              AND (:scopeOff = 1 OR cp.course_participant_id IN (:allowedIds))
            """;

    private static final String ID_FROM = """
            FROM course_participant cp
            JOIN participant p ON p.participant_id = cp.participant_id
            JOIN course c ON c.course_id = cp.course_id
            LEFT JOIN region r ON r.region_id = c.region_id
            LEFT JOIN region pr ON pr.region_id = r.parent_region_id
            """;

    private static final String COUNT_FROM = """
            FROM course_participant cp
            JOIN participant p ON p.participant_id = cp.participant_id
            JOIN course c ON c.course_id = cp.course_id
            """;

    @Override
    public Page<Long> findFilteredCourseParticipantIdsSorted(
            Long courseId,
            String status,
            int hasRegion,
            List<Long> regionIds,
            Integer courseNumber,
            Integer localCourseNumber,
            String keyword,
            LocalDate registerDateFrom,
            LocalDate registerDateTo,
            int scopeOff,
            List<Long> allowedIds,
            String sortBy,
            String sortOrder,
            Pageable pageable) {

        String orderBy = SortClauseBuilder.orderBy(SORT_WHITELIST, sortBy, sortOrder, DEFAULT_ORDER_BY, STABLE_KEY);

        String idSql = "SELECT cp.course_participant_id " + ID_FROM + WHERE_CONDITIONS + orderBy;
        Query idQuery = em.createNativeQuery(idSql);
        bindFilters(idQuery, courseId, status, hasRegion, regionIds, courseNumber, localCourseNumber,
                keyword, registerDateFrom, registerDateTo, scopeOff, allowedIds);
        // Pageable.unpaged()(집계·"전체 선택" 경로)는 getOffset()/getPageSize() 가 예외를 던지므로,
        // 페이징된 경우에만 offset/limit 을 적용한다(unpaged 는 전건 조회).
        if (pageable.isPaged()) {
            idQuery.setFirstResult((int) pageable.getOffset());
            idQuery.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Object> rows = idQuery.getResultList();
        List<Long> ids = rows.stream().map(value -> ((Number) value).longValue()).toList();

        String countSql = "SELECT COUNT(*) " + COUNT_FROM + WHERE_CONDITIONS;
        Query countQuery = em.createNativeQuery(countSql);
        bindFilters(countQuery, courseId, status, hasRegion, regionIds, courseNumber, localCourseNumber,
                keyword, registerDateFrom, registerDateTo, scopeOff, allowedIds);

        return PageableExecutionUtils.getPage(
                ids, pageable, () -> ((Number) countQuery.getSingleResult()).longValue());
    }

    // 기존 @Query 의 파라미터 목록과 1:1 대응 — 누락 시 런타임 오류이므로 정확히 유지한다.
    private void bindFilters(
            Query query,
            Long courseId,
            String status,
            int hasRegion,
            List<Long> regionIds,
            Integer courseNumber,
            Integer localCourseNumber,
            String keyword,
            LocalDate registerDateFrom,
            LocalDate registerDateTo,
            int scopeOff,
            List<Long> allowedIds) {
        query.setParameter("courseId", courseId);
        query.setParameter("status", status);
        query.setParameter("hasRegion", hasRegion);
        query.setParameter("regionIds", regionIds);
        query.setParameter("courseNumber", courseNumber);
        query.setParameter("localCourseNumber", localCourseNumber);
        query.setParameter("keyword", keyword);
        query.setParameter("registerDateFrom", registerDateFrom);
        query.setParameter("registerDateTo", registerDateTo);
        query.setParameter("scopeOff", scopeOff);
        query.setParameter("allowedIds", allowedIds);
    }
}
