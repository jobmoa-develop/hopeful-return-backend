package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseParticipantCounselorRepository
        extends JpaRepository<CourseParticipantCounselorEntity, Long> {

    List<CourseParticipantCounselorEntity> findByCourseParticipantId(Long courseParticipantId);

    List<CourseParticipantCounselorEntity> findByCounselorId(Long counselorId);

    Optional<CourseParticipantCounselorEntity> findByCourseParticipantIdAndStatus(
            Long courseParticipantId, CounselingType status);

    @EntityGraph(attributePaths = "counselor")
    List<CourseParticipantCounselorEntity> findByCourseParticipantIdIn(Collection<Long> courseParticipantIds);

    boolean existsByCourseParticipantIdAndCounselorIdAndStatus(
            Long courseParticipantId, Long counselorId, CounselingType status);

    /** 상담 구분(슬롯)에 무관하게 해당 상담사가 이 수강건에 배정돼 있는지 — 사후관리 조회 스코프 가드용. */
    boolean existsByCourseParticipantIdAndCounselorId(Long courseParticipantId, Long counselorId);

    void deleteByCourseParticipantId(Long courseParticipantId);
}
