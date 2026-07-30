package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseParticipantRepository extends JpaRepository<CourseParticipantEntity, Long> {

    List<CourseParticipantEntity> findByCourseId(Long courseId);

    // STAFF(진행자) 배정 회차 스코프 — 배정된 여러 회차의 참여자를 한 번에 조회한다.
    List<CourseParticipantEntity> findByCourseIdIn(Collection<Long> courseIds);

    @EntityGraph(attributePaths = "participant")
    List<CourseParticipantEntity> findWithParticipantByCourseId(Long courseId);

    @EntityGraph(attributePaths = "participant")
    List<CourseParticipantEntity> findWithParticipantByCourseParticipantIdIn(Collection<Long> courseParticipantIds);

    // 문자 발송 시 수신자별 {region}/{round} 치환용 — participant + course + region 을 함께 로드해 N+1 방지.
    @EntityGraph(attributePaths = {"participant", "course", "course.region"})
    List<CourseParticipantEntity> findWithParticipantAndCourseByCourseParticipantIdIn(
            Collection<Long> courseParticipantIds);

    @Query(value = "select cp from CourseParticipantEntity cp "
            + "left join cp.participant p "
            + "where cp.courseId = :courseId "
            + "and (:status is null or cp.status = :status) "
            + "and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))",
            countQuery = "select count(cp) from CourseParticipantEntity cp "
                    + "left join cp.participant p "
                    + "where cp.courseId = :courseId "
                    + "and (:status is null or cp.status = :status) "
                    + "and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))")
    Page<CourseParticipantEntity> findPageByCourseIdAndFilters(
            @Param("courseId") Long courseId,
            @Param("status") CourseParticipantStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("select cp from CourseParticipantEntity cp "
            + "join fetch cp.course c join fetch c.region "
            + "where cp.participantId in :participantIds")
    List<CourseParticipantEntity> findWithCourseByParticipantIdIn(
            @Param("participantIds") Collection<Long> participantIds);

    List<CourseParticipantEntity> findByParticipantId(Long participantId);

    List<CourseParticipantEntity> findByStatus(CourseParticipantStatus status);

    List<CourseParticipantEntity> findByCourseIdAndStatus(Long courseId, CourseParticipantStatus status);




    // 일괄 등록 중복 방지 — 같은 회차에 같은 참여자가 이미 등록돼 있는지 확인한다.
    boolean existsByCourseIdAndParticipantId(Long courseId, Long participantId);

    // 참여자 완전 삭제 전 회차 등록 이력 존재 확인(있으면 FK 안전상 삭제 차단).
    boolean existsByParticipantId(Long participantId);

    long countByCourseId(Long courseId);

    // ↓ dashboard 집계용 추가
    @EntityGraph(attributePaths = "course")
    List<CourseParticipantEntity> findByStatusIn(Collection<CourseParticipantStatus> statuses);

    List<CourseParticipantEntity> findByContactAttemptGreaterThanEqualAndStatusNotIn(
            Integer contactAttempt, Collection<CourseParticipantStatus> excludedStatuses);
}

