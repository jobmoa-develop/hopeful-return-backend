package com.jobmoa.hopefulreturn.course.repository;

import com.jobmoa.hopefulreturn.course.entity.CourseEntity;
import com.jobmoa.hopefulreturn.course.entity.CourseStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository extends JpaRepository<CourseEntity, Long>, JpaSpecificationExecutor<CourseEntity> {

    // 강좌 목록 조회 시 region 을 함께 로드해 toListItem 의 region 명 N+1 을 방지한다.
    // region 은 @ManyToOne → fetch join + 페이징 조합 안전(컬렉션 아님). Specification 동적 조건은 그대로 유지.
    @Override
    @EntityGraph(attributePaths = "region")
    Page<CourseEntity> findAll(Specification<CourseEntity> spec, Pageable pageable);

    List<CourseEntity> findByRegionId(Long regionId);

    List<CourseEntity> findByCreatedBy(Long createdBy);

    List<CourseEntity> findByStatus(CourseStatus status);

    List<CourseEntity> findByRegionIdAndCourseNumber(Long regionId, Integer courseNumber);

    // ↓ dashboard 집계용 추가
    long countByRegionIdAndStatus(Long regionId, CourseStatus status);

    List<CourseEntity> findByRecruitStartBetween(LocalDate from, LocalDate to);

    List<CourseEntity> findByRecruitEndBetween(LocalDate from, LocalDate to);

    List<CourseEntity> findByPlanSubmitDateBetween(LocalDate from, LocalDate to);
}