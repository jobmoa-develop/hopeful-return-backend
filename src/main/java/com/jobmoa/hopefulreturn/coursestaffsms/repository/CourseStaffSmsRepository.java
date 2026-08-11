package com.jobmoa.hopefulreturn.coursestaffsms.repository;

import com.jobmoa.hopefulreturn.coursestaffsms.entity.CourseStaffSmsEntity;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffNotifyType;
import com.jobmoa.hopefulreturn.coursestaffsms.entity.StaffSmsSendStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseStaffSmsRepository extends JpaRepository<CourseStaffSmsEntity, Long> {

    // 강좌 상세페이지(RoundDetailPage)에서 쓰는 단일 강좌 발송이력 — 기존 그대로 유지
    @Query("select s from CourseStaffSmsEntity s "
            + "left join fetch s.recipient recipient "
            + "left join fetch s.sender sender "
            + "where s.courseId = :courseId "
            + "order by s.courseStaffSmsId desc")
    List<CourseStaffSmsEntity> findByCourseIdWithUsers(@Param("courseId") Long courseId);

    // 전역 발송내역 조회(페이지·필터). sentBy=null 이면 전체(관리자), 값이 있으면 해당 발송자만.
    @Query(value = "select s from CourseStaffSmsEntity s "
            + "left join fetch s.recipient recipient "
            + "left join fetch s.sender sender "
            + "left join fetch s.course c "
            + "left join fetch c.region r "
            + "where (:sentBy is null or s.sentBy = :sentBy) "
            + "and (:notifyType is null or s.notifyType = :notifyType) "
            + "and (:sendStatus is null or s.sendStatus = :sendStatus) "
            + "and (:courseNumber is null or c.courseNumber = :courseNumber) "
            + "and (:localCourseNumber is null or c.localCourseNumber = :localCourseNumber) "
            + "and (:regionIds is null or c.regionId in :regionIds) "
            + "and (:dateFrom is null or s.sentAt >= :dateFrom) "
            + "and (:dateTo is null or s.sentAt < :dateTo) "
            + "and (:keyword is null or lower(recipient.name) like lower(concat('%', :keyword, '%')) "
            + "     or recipient.phone like concat('%', :keyword, '%')) "
            + "order by s.courseStaffSmsId desc",
            countQuery = "select count(s) from CourseStaffSmsEntity s "
                    + "left join s.recipient recipient "
                    + "left join s.course c "
                    + "where (:sentBy is null or s.sentBy = :sentBy) "
                    + "and (:notifyType is null or s.notifyType = :notifyType) "
                    + "and (:sendStatus is null or s.sendStatus = :sendStatus) "
                    + "and (:courseNumber is null or c.courseNumber = :courseNumber) "
                    + "and (:localCourseNumber is null or c.localCourseNumber = :localCourseNumber) "
                    + "and (:regionIds is null or c.regionId in :regionIds) "
                    + "and (:dateFrom is null or s.sentAt >= :dateFrom) "
                    + "and (:dateTo is null or s.sentAt < :dateTo) "
                    + "and (:keyword is null or lower(recipient.name) like lower(concat('%', :keyword, '%')) "
                    + "     or recipient.phone like concat('%', :keyword, '%'))")
    Page<CourseStaffSmsEntity> findPageByFilters(
            @Param("sentBy") Long sentBy,
            @Param("notifyType") StaffNotifyType notifyType,
            @Param("sendStatus") StaffSmsSendStatus sendStatus,
            @Param("courseNumber") Integer courseNumber,
            @Param("localCourseNumber") Integer localCourseNumber,
            @Param("regionIds") List<Long> regionIds,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("keyword") String keyword,
            Pageable pageable);
}