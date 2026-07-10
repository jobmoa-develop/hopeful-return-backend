package com.jobmoa.hopefulreturn.staffschedule.repository;

import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffScheduleRepository extends JpaRepository<StaffScheduleEntity, Long> {

    // 단건/일괄 등록 시 UNIQUE(user_id, schedule_date, session_type) 중복 검사
    boolean existsByUserIdAndScheduleDateAndSessionType(
            Long userId, LocalDate scheduleDate, SessionType sessionType);

    // 내 캘린더 범위 조회(/me)
    List<StaffScheduleEntity> findByUserIdAndScheduleDateBetween(
            Long userId, LocalDate fromDate, LocalDate toDate);

    // 배정 저장 upsert 시 기존 행 조회(UNIQUE 키)
    Optional<StaffScheduleEntity> findByUserIdAndScheduleDateAndSessionType(
            Long userId, LocalDate scheduleDate, SessionType sessionType);

    // 회차 배정 행 — 해당 회차 course_staff id 집합에 연결된 일정
    List<StaffScheduleEntity> findByCourseStaffIdIn(List<Long> courseStaffIds);

    // 근무 불가일(배정 아님 + is_available=false) — 후보 필터용
    List<StaffScheduleEntity> findByScheduleDateBetweenAndIsAvailableFalseAndCourseStaffIdIsNull(
            LocalDate fromDate, LocalDate toDate);
}
