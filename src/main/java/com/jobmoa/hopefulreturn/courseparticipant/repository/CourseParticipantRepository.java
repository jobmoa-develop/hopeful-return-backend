package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseParticipantRepository extends JpaRepository<CourseParticipantEntity, Long> {

    List<CourseParticipantEntity> findByCourseId(Long courseId);

    @EntityGraph(attributePaths = "participant")
    List<CourseParticipantEntity> findWithParticipantByCourseId(Long courseId);

    @Query("select cp from CourseParticipantEntity cp "
            + "join fetch cp.course c join fetch c.region "
            + "where cp.participantId in :participantIds")
    List<CourseParticipantEntity> findWithCourseByParticipantIdIn(
            @Param("participantIds") Collection<Long> participantIds);

    List<CourseParticipantEntity> findByParticipantId(Long participantId);

    List<CourseParticipantEntity> findByStatus(CourseParticipantStatus status);

    List<CourseParticipantEntity> findByCourseIdAndStatus(Long courseId, CourseParticipantStatus status);

    long countByCourseId(Long courseId);   // 추가
}
