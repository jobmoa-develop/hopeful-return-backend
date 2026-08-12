package com.jobmoa.hopefulreturn.courseparticipant.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 상담/수강생 목록(ConsultingPage) 조회의 동적 정렬을 위한 커스텀 fragment.
 *
 * <p>기존 네이티브 @Query 로는 ORDER BY 를 파라미터로 바꿀 수 없어, sortBy/sortOrder 를
 * 화이트리스트로 검증해 안전하게 조립하는 이 구현으로 대체한다. 정렬·필터는 DB 에서 처리하고
 * courseParticipantId 목록만 페이징해 반환한다(상세 데이터는 이 ID 로 재조회).
 */
public interface CourseParticipantRepositoryCustom {

    Page<Long> findFilteredCourseParticipantIdsSorted(
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
            Pageable pageable);
}
