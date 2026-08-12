package com.jobmoa.hopefulreturn.courseparticipant.repository;

import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseParticipantRepository
        extends JpaRepository<CourseParticipantEntity, Long>, CourseParticipantRepositoryCustom {

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

    // ── 이하 신규 추가 ────────────────────────────────────────────────
    // 상담 목록/수강생 목록 조회 — 지역 표시순서(부모→자식 region_id 오름차순) 1순위,
    // 참여자 이름 가나다순 2순위, courseParticipantId 3순위(안정 정렬)로 정렬해 페이징 반환한다.
    // regionIds/allowedIds가 비어있을 수 없으므로(IN () 문법 오류 방지), hasRegion=0/scopeOff=1일 때는
    // 호출부(Service)에서 더미값(List.of(-1L))을 채워 넣고 해당 조건절 자체를 우회시킨다.
    @Query(value = """
        SELECT cp.*
        FROM course_participant cp
        JOIN participant p ON p.participant_id = cp.participant_id
        JOIN course c ON c.course_id = cp.course_id
        JOIN region r ON r.region_id = c.region_id
        LEFT JOIN region pr ON pr.region_id = r.parent_region_id
        WHERE (:courseId IS NULL OR cp.course_id = :courseId)
          AND (:status IS NULL OR cp.status = :status)
          AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
          AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
          AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
          AND (:keyword IS NULL OR p.name LIKE '%' + :keyword + '%' OR p.phone LIKE '%' + :keyword + '%')
          AND (:registerDateFrom IS NULL OR CAST(cp.created_at AS DATE) >= :registerDateFrom)
          AND (:registerDateTo IS NULL OR CAST(cp.created_at AS DATE) <= :registerDateTo)
          AND (:scopeOff = 1 OR cp.course_participant_id IN (:allowedIds))
        ORDER BY pr.region_id, r.region_id,
                 p.name COLLATE SQL_Latin1_General_CP1_CI_AS,
                 cp.course_participant_id
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM course_participant cp
        JOIN participant p ON p.participant_id = cp.participant_id
        JOIN course c ON c.course_id = cp.course_id
        WHERE (:courseId IS NULL OR cp.course_id = :courseId)
          AND (:status IS NULL OR cp.status = :status)
          AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
          AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
          AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
          AND (:keyword IS NULL OR p.name LIKE '%' + :keyword + '%' OR p.phone LIKE '%' + :keyword + '%')
          AND (:registerDateFrom IS NULL OR CAST(cp.created_at AS DATE) >= :registerDateFrom)
          AND (:registerDateTo IS NULL OR CAST(cp.created_at AS DATE) <= :registerDateTo)
          AND (:scopeOff = 1 OR cp.course_participant_id IN (:allowedIds))
        """,
            nativeQuery = true)
    Page<CourseParticipantEntity> findAllSortedByRegionAndName(
            @Param("courseId") Long courseId,
            @Param("status") String status,
            @Param("hasRegion") int hasRegion,
            @Param("regionIds") List<Long> regionIds,
            @Param("courseNumber") Integer courseNumber,
            @Param("localCourseNumber") Integer localCourseNumber,
            @Param("keyword") String keyword,
            @Param("registerDateFrom") LocalDate registerDateFrom,
            @Param("registerDateTo") LocalDate registerDateTo,
            @Param("scopeOff") int scopeOff,
            @Param("allowedIds") List<Long> allowedIds,
            Pageable pageable);

    // QR 공개 입·퇴실 본인확인 — 회차 스코프 + 성명 일치 + 전화번호 뒤 4자리 매칭을 DB 에서 수행한다.
    // 하이픈(-)만 제거한 뒤 RIGHT(...,4) 로 뒷자리를 비교하며, 취소(CANCELED) 등록은 제외한다.
    // 정확히 1명일 때만 통과시키는 판정은 호출부(QrAttendanceServiceImpl)에서 수행한다.
    @Query(value = "SELECT cp.* FROM course_participant cp "
            + "JOIN participant p ON p.participant_id = cp.participant_id "
            + "WHERE cp.course_id = :courseId AND p.name = :name "
            + "AND RIGHT(REPLACE(p.phone, '-', ''), 4) = :last4 "
            + "AND cp.status <> 'CANCELED'",
            nativeQuery = true)
    List<CourseParticipantEntity> findForQrVerify(
            @Param("courseId") Long courseId,
            @Param("name") String name,
            @Param("last4") String last4);

    // 강좌별 참여자 수 집계 — courseId 로 group by 하여 [courseId, count] 쌍 목록을 반환한다.
    // CourseServiceImpl.participantCountsByCourseId() 가 강좌 목록 조회 시 N+1 없이 배치 집계할 때 사용.
    @Query("select cp.courseId, count(cp) from CourseParticipantEntity cp "
            + "where cp.courseId in :courseIds group by cp.courseId")
    List<Object[]> countByCourseIdIn(@Param("courseIds") Collection<Long> courseIds);

    // 상담 목록(ConsultingPage) 조회의 동적 정렬은 CourseParticipantRepositoryCustom /
    // CourseParticipantRepositoryImpl 로 이관했다(ORDER BY 를 sortBy/sortOrder 로 화이트리스트 조립).
}