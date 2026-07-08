package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseParticipantRepository extends JpaRepository<CourseParticipantEntity, Long> {

    List<CourseParticipantEntity> findByCourseId(Long courseId);

    List<CourseParticipantEntity> findByParticipantId(Long participantId);

    List<CourseParticipantEntity> findByStatus(CourseParticipantStatus status);

    List<CourseParticipantEntity> findByCourseIdAndStatus(Long courseId, CourseParticipantStatus status);
}
