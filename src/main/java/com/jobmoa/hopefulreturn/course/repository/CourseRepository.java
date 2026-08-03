package com.jobmoa.hopefulreturn.course.repository;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository extends JpaRepository<CourseEntity, Long>, JpaSpecificationExecutor<CourseEntity> {

    List<CourseEntity> findByRegionId(Long regionId);

    List<CourseEntity> findByCreatedBy(Long createdBy);

    List<CourseEntity> findByStatus(CourseStatus status);

    List<CourseEntity> findByRegionIdAndCourseNumber(Long regionId, Integer courseNumber);

    // ↓ dashboard 집계용 추가
    long countByRegionIdAndStatus(Long regionId, CourseStatus status);

    List<CourseEntity> findByRecruitStartBetween(LocalDate from, LocalDate to);

    List<CourseEntity> findByRecruitEndBetween(LocalDate from, LocalDate to);

    List<CourseEntity> findByPlanSubmitDateBetween(LocalDate from, LocalDate to);
}