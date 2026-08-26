package com.jobmoa.hopefulreturn.participant.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 참여자 목록(ParticipantsPage) 조회의 동적 정렬을 위한 커스텀 fragment.
 *
 * <p>목록 단위는 <b>수강건(course_participant)</b>이다 — 참여자가 여러 회차에 등록됐으면 수강건마다
 * 1행({@link ParticipantEnrollmentRef})이 나오고, 등록 이력이 없는 참여자는 courseParticipantId 가
 * null 인 1행으로 나온다. sortBy/sortOrder 는 화이트리스트로 검증해 안전하게 ORDER BY 로 조립한다.
 */
public interface ParticipantRepositoryCustom {

    Page<ParticipantEnrollmentRef> findFilteredEnrollmentRefsSorted(
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
