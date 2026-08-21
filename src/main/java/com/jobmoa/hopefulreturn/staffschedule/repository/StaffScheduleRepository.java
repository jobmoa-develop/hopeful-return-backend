package com.jobmoa.hopefulreturn.staffschedule.repository;

import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import com.jobmoa.hopefulreturn.coursestaff.entity.SessionType;
import com.jobmoa.hopefulreturn.role.entity.RoleName;
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

    // 근무 가용/불가 판정용 — 배정 아님(course_staff_id NULL) 행 전체(is_available 무관).
    // 세션 우선순위 해석(구체 세션 AM/PM 이 FULL 을 그 세션에 한해 override)을 위해
    // is_available=true 세션 행(예: '오후 가능')까지 함께 읽어야 하므로 false 로 한정하지 않는다.
    List<StaffScheduleEntity> findByScheduleDateBetweenAndCourseStaffIdIsNull(
            LocalDate fromDate, LocalDate toDate);

    /**
     * 상담사 일정 캘린더 오버레이용 — <b>상담사(COUNSELOR) 역할 사용자 본인</b>의 근무 불가일
     * (배정 아님: course_staff_id NULL + is_available=false)을 기간 내에서 조회한다.
     * 지역·회차 속성이 없는 일정이라 해당 필터는 적용하지 않으며, 이름 부분일치(LIKE)만 선택 적용한다.
     * user 를 fetch 하여 상담사명을 함께 로드하고, exists 서브쿼리로 역할을 제한한다(행 증식 방지).
     */
    @Query("select ss from StaffScheduleEntity ss "
            + "join fetch ss.user u "
            + "where ss.isAvailable = false "
            + "and ss.courseStaffId is null "
            + "and ss.scheduleDate between :from and :to "
            + "and (:counselorPattern is null or lower(u.name) like lower(:counselorPattern)) "
            + "and exists (select 1 from UserRoleEntity ur join ur.role r "
            + "            where ur.userId = ss.userId and r.roleName = :roleName) "
            + "order by ss.scheduleDate asc, u.name asc")
    List<StaffScheduleEntity> findCounselorUnavailabilities(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("counselorPattern") String counselorPattern,
            @Param("roleName") RoleName roleName);

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
