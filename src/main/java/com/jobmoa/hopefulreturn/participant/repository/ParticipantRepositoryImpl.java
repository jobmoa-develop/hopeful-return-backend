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
 * {@link ParticipantRepositoryCustom} 구현 — 참여자 목록을 <b>수강건 단위</b>로 조회·정렬한다.
 *
 * <p>참여자별 "최신 수강건 1건" dedup(과거 latest CTE + ROW_NUMBER rn=1)을 제거하고,
 * course_participant 를 직접 LEFT JOIN 해 수강건마다 한 행을 반환한다. 수강건이 없는 참여자는
 * LEFT JOIN 특성상 courseParticipantId 가 null 인 한 행으로 보존된다. ORDER BY 만
 * {@link SortClauseBuilder} 로 화이트리스트 기반 조립하며, 정렬 미지정 시 기존 기본 정렬
 * (지역 표시순 → 이름)로 폴백한다.
 */
public class ParticipantRepositoryImpl implements ParticipantRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    // 정렬 화이트리스트 — 키(FE 전달값) → 하드코딩된 컬럼식. 원문 sortBy 는 SQL 에 삽입하지 않는다.
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "name", "p.name COLLATE SQL_Latin1_General_CP1_CI_AS",
            "phone", "p.phone",
            "region", "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id",
            "registerDate", "cp.created_at");

    // 정렬 미지정 시 사용할 기본 정렬(지역 표시순 → 이름). 수강건 단위라 같은 참여자의 여러 행이
    // 인접·안정 정렬되도록 participant_id → course_participant_id tiebreaker 를 본문에 포함한다
    // (폴백 경로는 SortClauseBuilder 가 stableKey 를 부착하지 않으므로 상수에 직접 넣는다).
    private static final String DEFAULT_ORDER_BY =
            "CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END, pr.region_id, r.region_id, "
                    + "p.name COLLATE SQL_Latin1_General_CP1_CI_AS, p.participant_id, cp.course_participant_id";
    // 정렬 지정 시 tiebreaker 로 붙일 안정키 — 같은 참여자 행을 인접시키고 수강건 단위로 결정적 순서 보장.
    private static final String STABLE_KEY = "p.participant_id, cp.course_participant_id";

    // WHERE 조건절 — p/c/cp 만 참조하므로 ID 쿼리(지역 조인 포함)·COUNT 쿼리(지역 조인 없음) 양쪽에 재사용.
    private static final String WHERE_CONDITIONS = """
            WHERE (:name IS NULL OR p.name LIKE '%' + :name + '%')
              AND (:phone IS NULL OR p.phone LIKE '%' + :phone + '%')
              AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
              AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
              AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
              AND (:registerDateFrom IS NULL OR CAST(cp.created_at AS DATE) >= :registerDateFrom)
              AND (:registerDateTo IS NULL OR CAST(cp.created_at AS DATE) <= :registerDateTo)
              AND (:scopeOff = 1 OR p.participant_id IN (:allowedIds))
            """;

    private static final String ID_FROM = """
            SELECT p.participant_id, cp.course_participant_id
            FROM participant p
            LEFT JOIN course_participant cp ON cp.participant_id = p.participant_id
            LEFT JOIN course c ON c.course_id = cp.course_id
            LEFT JOIN region r ON r.region_id = c.region_id
            LEFT JOIN region pr ON pr.region_id = r.parent_region_id
            """;

    private static final String COUNT_FROM = """
            SELECT COUNT(*)
            FROM participant p
            LEFT JOIN course_participant cp ON cp.participant_id = p.participant_id
            LEFT JOIN course c ON c.course_id = cp.course_id
            """;

    @Override
    public Page<ParticipantEnrollmentRef> findFilteredEnrollmentRefsSorted(
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

        String idSql = ID_FROM + WHERE_CONDITIONS + orderBy;
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
        List<Object[]> rows = idQuery.getResultList();
        List<ParticipantEnrollmentRef> refs = rows.stream()
                .map(row -> new ParticipantEnrollmentRef(
                        ((Number) row[0]).longValue(),
                        row[1] == null ? null : ((Number) row[1]).longValue()))
                .toList();

        String countSql = COUNT_FROM + WHERE_CONDITIONS;
        Query countQuery = em.createNativeQuery(countSql);
        bindFilters(countQuery, name, phone, hasRegion, regionIds, courseNumber, localCourseNumber,
                registerDateFrom, registerDateTo, scopeOff, allowedIds);

        return PageableExecutionUtils.getPage(
                refs, pageable, () -> ((Number) countQuery.getSingleResult()).longValue());
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
