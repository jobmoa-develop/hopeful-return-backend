package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseParticipantCounselorRepository
        extends JpaRepository<CourseParticipantCounselorEntity, Long> {

    List<CourseParticipantCounselorEntity> findByCourseParticipantId(Long courseParticipantId);

    List<CourseParticipantCounselorEntity> findByCounselorId(Long counselorId);

    boolean existsByCourseParticipantIdAndCounselorIdAndStatus(
            Long courseParticipantId, Long counselorId, CounselingType status);

    void deleteByCourseParticipantId(Long courseParticipantId);
}
