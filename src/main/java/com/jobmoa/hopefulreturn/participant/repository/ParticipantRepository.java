package com.jobmoa.hopefulreturn.participant.repository;

import com.jobmoa.hopefulreturn.participant.entity.ParticipantEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<ParticipantEntity, Long> {

    List<ParticipantEntity> findByPhone(String phone);

    Optional<ParticipantEntity> findByMatchKey(String matchKey);

    List<ParticipantEntity> findByName(String name);

    boolean existsByPhone(String phone);

    Optional<ParticipantEntity> findFirstByPhoneOrderByParticipantIdAsc(String phone);

    Page<ParticipantEntity> findByNameContaining(String name, Pageable pageable);

    Page<ParticipantEntity> findByPhoneContaining(String phone, Pageable pageable);

    Page<ParticipantEntity> findByNameContainingAndPhoneContaining(String name, String phone, Pageable pageable);

    // ── 이하 신규 추가 ────────────────────────────────────────────────
    // 참여자별 "최신 수강건"(course_participant_id 최댓값) 기준으로 지역·회차·등록일 필터를 적용하고,
    // 지역 표시순서(부모→자식 region_id 오름차순) 1순위, 이름 가나다순 2순위로 정렬한 participant_id 를
    // 페이징해 반환한다. 최신 수강건이 없는 참여자(회차 미등록)는 지역 정렬상 맨 뒤로 밀린다.
    // 상세 데이터(상담사·출결 요약 등)는 이 ID 목록으로 기존 배치 조회 로직을 그대로 재사용한다.
    @Query(value = """
        WITH latest AS (
            SELECT cp.*, ROW_NUMBER() OVER (
                     PARTITION BY cp.participant_id
                     ORDER BY cp.course_participant_id DESC) AS rn
            FROM course_participant cp
        )
        SELECT p.participant_id
        FROM participant p
        LEFT JOIN latest l ON l.participant_id = p.participant_id AND l.rn = 1
        LEFT JOIN course c ON c.course_id = l.course_id
        LEFT JOIN region r ON r.region_id = c.region_id
        LEFT JOIN region pr ON pr.region_id = r.parent_region_id
        WHERE (:name IS NULL OR p.name LIKE '%' + :name + '%')
          AND (:phone IS NULL OR p.phone LIKE '%' + :phone + '%')
          AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
          AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
          AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
          AND (:registerDateFrom IS NULL OR CAST(l.created_at AS DATE) >= :registerDateFrom)
          AND (:registerDateTo IS NULL OR CAST(l.created_at AS DATE) <= :registerDateTo)
          AND (:scopeOff = 1 OR p.participant_id IN (:allowedIds))
        ORDER BY CASE WHEN r.region_id IS NULL THEN 1 ELSE 0 END,
                 pr.region_id, r.region_id,
                 p.name COLLATE SQL_Latin1_General_CP1_CI_AS
        """,
            countQuery = """
        WITH latest AS (
            SELECT cp.*, ROW_NUMBER() OVER (
                     PARTITION BY cp.participant_id
                     ORDER BY cp.course_participant_id DESC) AS rn
            FROM course_participant cp
        )
        SELECT COUNT(*)
        FROM participant p
        LEFT JOIN latest l ON l.participant_id = p.participant_id AND l.rn = 1
        LEFT JOIN course c ON c.course_id = l.course_id
        WHERE (:name IS NULL OR p.name LIKE '%' + :name + '%')
          AND (:phone IS NULL OR p.phone LIKE '%' + :phone + '%')
          AND (:hasRegion = 0 OR c.region_id IN (:regionIds))
          AND (:courseNumber IS NULL OR c.course_number = :courseNumber)
          AND (:localCourseNumber IS NULL OR c.local_course_number = :localCourseNumber)
          AND (:registerDateFrom IS NULL OR CAST(l.created_at AS DATE) >= :registerDateFrom)
          AND (:registerDateTo IS NULL OR CAST(l.created_at AS DATE) <= :registerDateTo)
          AND (:scopeOff = 1 OR p.participant_id IN (:allowedIds))
        """,
            nativeQuery = true)
    Page<Long> findFilteredParticipantIdsSorted(
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("hasRegion") int hasRegion,
            @Param("regionIds") List<Long> regionIds,
            @Param("courseNumber") Integer courseNumber,
            @Param("localCourseNumber") Integer localCourseNumber,
            @Param("registerDateFrom") LocalDate registerDateFrom,
            @Param("registerDateTo") LocalDate registerDateTo,
            @Param("scopeOff") int scopeOff,
            @Param("allowedIds") List<Long> allowedIds,
            Pageable pageable);
}