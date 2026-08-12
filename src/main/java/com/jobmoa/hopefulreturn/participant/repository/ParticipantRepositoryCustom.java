package com.jobmoa.hopefulreturn.participant.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 참여자 목록(ParticipantsPage) 조회의 동적 정렬을 위한 커스텀 fragment.
 *
 * <p>기존 네이티브 @Query(CTE + ROW_NUMBER 최신 수강건) 로는 ORDER BY 를 파라미터로 바꿀 수 없어,
 * sortBy/sortOrder 를 화이트리스트로 검증해 안전하게 조립하는 이 구현으로 대체한다.
 */
public interface ParticipantRepositoryCustom {

    Page<Long> findFilteredParticipantIdsSorted(
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
            Pageable pageable);
}
