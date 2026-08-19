package com.jobmoa.hopefulreturn.coursedailycounselor.repository;

import com.jobmoa.hopefulreturn.coursedailycounselor.entity.CourseDailyCounselorEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseDailyCounselorRepository
        extends JpaRepository<CourseDailyCounselorEntity, Long> {

    /**
     * 회차의 상담사 일별 배정을 조회한다(그리드 복원용). course_staff·users 를 fetch 하여
     * 회차·인력명·전화번호를 함께 로드한다.
     */
    @Query("select cdc from CourseDailyCounselorEntity cdc "
            + "join fetch cdc.courseStaff cs "
            + "join fetch cs.user u "
            + "where cs.courseId = :courseId "
            + "order by cdc.scheduleDate asc, cdc.courseDailyCounselorId asc")
    List<CourseDailyCounselorEntity> findByCourseId(@Param("courseId") Long courseId);

    // 저장 시 대상 상담사 로스터의 기존 일별 배정 조회/삭제
    List<CourseDailyCounselorEntity> findByCourseStaffIdIn(List<Long> courseStaffIds);

    void deleteByCourseStaffIdIn(List<Long> courseStaffIds);

    /**
     * 상담사 본인의 일별 배정을 기간 내에서 조회한다(개인 캘린더 /me 병합용). course_staff 를
     * fetch 하여 회차·역할·세션을 함께 로드한다.
     */
    @Query("select cdc from CourseDailyCounselorEntity cdc "
            + "join fetch cdc.courseStaff cs "
            + "join fetch cs.user u "
            + "where cs.userId = :userId and cdc.scheduleDate between :from and :to")
    List<CourseDailyCounselorEntity> findByUserIdAndScheduleDateBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
