package com.jobmoa.hopefulreturn.staffschedule.repository;

import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.staffschedule.entity.StaffScheduleEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 대상 인력들이 <b>현재 회차가 아닌 폐강되지 않은 다른 회차</b>에 이미 배정된 행(배정 행:
     * course_staff_id NOT NULL) 을 기간 내에서 조회한다. 중복 방지(후보 busy)·저장 충돌 검사용.
     * course_staff·course 를 fetch 하여 회차명·역할·상태를 함께 로드한다.
     */
    @Query("select ss from StaffScheduleEntity ss "
            + "join fetch ss.courseStaff cs join fetch cs.course c "
            + "where ss.userId in :userIds and ss.scheduleDate between :from and :to "
            + "and ss.courseStaffId is not null and cs.courseId <> :courseId "
            + "and c.status <> :canceledStatus")
    List<StaffScheduleEntity> findOtherActiveAssignments(
            @Param("userIds") List<Long> userIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("courseId") Long courseId,
            @Param("canceledStatus") CourseStatus canceledStatus);
}
