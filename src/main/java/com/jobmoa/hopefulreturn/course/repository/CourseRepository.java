package com.jobmoa.hopefulreturn.course.repository;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findByRegionId(Long regionId);

    List<CourseEntity> findByCreatedBy(Long createdBy);

    List<CourseEntity> findByStatus(CourseStatus status);

    List<CourseEntity> findByRegionIdAndCourseNumber(Long regionId, Integer courseNumber);
}
