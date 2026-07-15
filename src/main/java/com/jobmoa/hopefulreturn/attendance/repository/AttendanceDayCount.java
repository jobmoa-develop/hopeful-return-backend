package com.jobmoa.hopefulreturn.attendance.repository;

/**
 * 수강건별 출석 일수 집계 프로젝션 (출석·지각을 출석으로 집계).
 */
public interface AttendanceDayCount {

    Long getCourseParticipantId();

    Long getAttendedDays();
}
