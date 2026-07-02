package com.jobmoa.hopefulreturn.coursestaff.repository;

import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseStaffRepository extends JpaRepository<CourseStaffEntity, Long> {

    List<CourseStaffEntity> findByCourseId(Long courseId);

    List<CourseStaffEntity> findByUserId(Long userId);

    List<CourseStaffEntity> findByStaffRole(StaffRole staffRole);

    List<CourseStaffEntity> findByCourseIdAndStaffRole(Long courseId, StaffRole staffRole);
}
