package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CounselingType;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantCounselorEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 상담 일정 캘린더용 — 상담 시작일(counseling_started_at)이 있는 슬롯을 회차·상담사 필터와 함께 조회한다.
     * 참여자·회차·지역·상담사를 fetch join(N+1 방지). 전 회차·전 상담사 노출(스코프 미적용).
     * 날짜 범위는 [from, toExclusive) — 서비스에서 종료일+1일 자정을 넘겨 포함 범위를 만든다.
     * counselorPattern 은 서비스에서 만든 "%검색어%" 형태의 LIKE 패턴(없으면 null). concat 을 쓰면
     * SQL Server 방언이 파라미터를 varchar(max) 로 캐스팅해 한글이 유실되므로 패턴을 미리 만들어 넘긴다.
     */
    @Query("select cpc from CourseParticipantCounselorEntity cpc "
            + "join fetch cpc.counselor u "
            + "join fetch cpc.courseParticipant cp "
            + "join fetch cp.participant p "
            + "join fetch cp.course c "
            + "join fetch c.region r "
            + "where cpc.counselingStartedAt is not null "
            + "and cpc.counselingStartedAt >= :from and cpc.counselingStartedAt < :toExclusive "
            + "and (:regionIds is null or c.regionId in :regionIds) "
            + "and (:courseNumber is null or c.courseNumber = :courseNumber) "
            + "and (:localCourseNumber is null or c.localCourseNumber = :localCourseNumber) "
            + "and (:counselorPattern is null or lower(u.name) like lower(:counselorPattern)) "
            + "order by cpc.counselingStartedAt asc")
    List<CourseParticipantCounselorEntity> findCounselingSchedules(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("regionIds") List<Long> regionIds,
            @Param("courseNumber") Integer courseNumber,
            @Param("localCourseNumber") Integer localCourseNumber,
            @Param("counselorPattern") String counselorPattern);
}
