package com.jobmoa.hopefulreturn.coursedailystaff.service;

import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignConflict;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffCandidateResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.CourseDailyStaffListResponse;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffRequest;
import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.SaveCourseDailyStaffResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CourseDailyStaffService {

    CourseDailyStaffListResponse findAll(Long courseId);

    SaveCourseDailyStaffResponse save(SaveCourseDailyStaffRequest request);

    CourseDailyStaffCandidateResponse findCandidates(Long courseId);

    /**
     * 회차 교육일자 변경에 맞춰 그 회차에 배정된 인력의 일정을 재동기화한다.
     *   - {@code moved}: 옛 교육일 → 새 교육일. 배정 행의 scheduleDate 를 옛→새로 이동한다.
     *   - {@code removed}: 삭제된 옛 교육일. 그 날 배정을 자동 해제한다
     *     (staff_schedule 은 course_staff_id=null, course_daily_counselor 는 행 삭제).
     *
     * <p>PM(course_staff 단위·날짜 없음)과 배정 아님(course_staff_id NULL) 개인 일정은 대상이 아니다.
     * 이동 대상 날짜가 이미 점유(타 회차 배정·개인 불가일 등)돼 UNIQUE 충돌이 나는 행은 건너뛴다.
     */
    void remapAssignmentDates(Long courseId, Map<LocalDate, LocalDate> moved, Set<LocalDate> removed);

    /**
     * 교육일 이동({@code moved}: 옛→새) 시, 이 회차에 배정된 인력(비-PM·비-상담사)이 <b>새 날짜</b>에서
     * 다른 일정과 겹치는 충돌 목록을 반환한다. 판정: (a) 폐강 아닌 다른 회차에 같은 날·겹치는 시간대로
     * 배정됨, (b) 본인 근무 불가일. 겹침이 없으면 빈 목록. 회차 수정 시 확인(confirmConflicts) 흐름의
     * 사전 감지에 쓴다. 상담사는 같은 날 다중 회차 배정이 허용되므로 대상이 아니다.
     */
    List<AssignConflict> detectDateChangeConflicts(Long courseId, Map<LocalDate, LocalDate> moved);
}
