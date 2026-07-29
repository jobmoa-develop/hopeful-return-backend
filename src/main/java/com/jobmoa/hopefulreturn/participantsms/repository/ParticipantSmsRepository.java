package com.jobmoa.hopefulreturn.participantsms.repository;

import com.jobmoa.hopefulreturn.participantsms.entity.ParticipantSmsEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
import com.jobmoa.hopefulreturn.participantsms.entity.SmsType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantSmsRepository extends JpaRepository<ParticipantSmsEntity, Long> {

    List<ParticipantSmsEntity> findByCourseParticipantId(Long courseParticipantId);

    List<ParticipantSmsEntity> findByCourseParticipantIdOrderBySmsIdDesc(Long courseParticipantId);

    List<ParticipantSmsEntity> findBySentBy(Long sentBy);

    List<ParticipantSmsEntity> findBySmsType(SmsType smsType);

    List<ParticipantSmsEntity> findBySendStatus(SendStatus sendStatus);

    // 발송결과 폴링 대상: request_id 가 있고(실 SENS 발송) cutoff 이내인 PENDING 행만.
    List<ParticipantSmsEntity> findBySendStatusAndRequestIdIsNotNullAndSentAtAfter(
            SendStatus sendStatus, LocalDateTime sentAtAfter);

    // 전역 발송내역 조회(페이지·필터). sentBy=null 이면 전체(관리자), 값이 있으면 해당 발송자만.
    // 단일값 연관만 fetch join 하므로 Pageable 과 함께 써도 in-memory 페이징 경고가 없다.
    @Query(value = "select ps from ParticipantSmsEntity ps "
            + "left join fetch ps.courseParticipant cp "
            + "left join fetch cp.participant p "
            + "left join fetch cp.course c "
            + "left join fetch c.region r "
            + "left join fetch ps.sender su "
            + "where (:sentBy is null or ps.sentBy = :sentBy) "
            + "and (:sendStatus is null or ps.sendStatus = :sendStatus) "
            + "and (:courseNumber is null or c.courseNumber = :courseNumber) "
            + "and (:regionIds is null or c.regionId in :regionIds) "
            + "and (:dateFrom is null or ps.sentAt >= :dateFrom) "
            + "and (:dateTo is null or ps.sentAt < :dateTo) "
            + "and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')) "
            + "     or p.phone like concat('%', :keyword, '%'))",
            countQuery = "select count(ps) from ParticipantSmsEntity ps "
                    + "left join ps.courseParticipant cp "
                    + "left join cp.participant p "
                    + "left join cp.course c "
                    + "where (:sentBy is null or ps.sentBy = :sentBy) "
                    + "and (:sendStatus is null or ps.sendStatus = :sendStatus) "
                    + "and (:courseNumber is null or c.courseNumber = :courseNumber) "
                    + "and (:regionIds is null or c.regionId in :regionIds) "
                    + "and (:dateFrom is null or ps.sentAt >= :dateFrom) "
                    + "and (:dateTo is null or ps.sentAt < :dateTo) "
                    + "and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')) "
                    + "     or p.phone like concat('%', :keyword, '%'))")
    Page<ParticipantSmsEntity> findPageByFilters(
            @Param("sentBy") Long sentBy,
            @Param("sendStatus") SendStatus sendStatus,
            @Param("courseNumber") Integer courseNumber,
            @Param("regionIds") List<Long> regionIds,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("keyword") String keyword,
            Pageable pageable);
}
