package com.jobmoa.hopefulreturn.coursestaff.repository;

import com.jobmoa.hopefulreturn.coursestaff.entity.CourseStaffEntity;
import com.jobmoa.hopefulreturn.coursestaff.entity.StaffRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseStaffRepository extends JpaRepository<CourseStaffEntity, Long> {

    List<CourseStaffEntity> findByCourseId(Long courseId);

    /**
     * 배정 ID 묶음으로 course·region 을 한 번에 조인 조회한다(배정 회차명 표시용 N+1 방지).
     * region_id 는 NOT NULL 이나, 방어적으로 left join 하여 데이터 이상 시에도 누락 없이 반환한다.
     */
    @Query("select cs from CourseStaffEntity cs "
            + "left join fetch cs.course c "
            + "left join fetch c.region r "
            + "where cs.courseStaffId in :ids")
    List<CourseStaffEntity> findWithCourseAndRegionByIdIn(@Param("ids") List<Long> ids);

    List<CourseStaffEntity> findByCourseIdOrderByCourseStaffIdAsc(Long courseId);

    List<CourseStaffEntity> findByUserId(Long userId);

    List<CourseStaffEntity> findByStaffRole(StaffRole staffRole);

    List<CourseStaffEntity> findByCourseIdAndStaffRole(Long courseId, StaffRole staffRole);
}
