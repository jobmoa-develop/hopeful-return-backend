package com.jobmoa.hopefulreturn.adminsms.repository;

import com.jobmoa.hopefulreturn.adminsms.entity.AdminSmsEntity;
import com.jobmoa.hopefulreturn.participantsms.entity.SendStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminSmsRepository extends JpaRepository<AdminSmsEntity, Long> {

    // 발송결과 폴링 대상: request_id 가 있고(실 SENS 발송) cutoff 이내인 PENDING 행만.
    List<AdminSmsEntity> findBySendStatusAndRequestIdIsNotNullAndSentAtAfter(
            SendStatus sendStatus, LocalDateTime sentAtAfter);

    // 예약 승격 대상: 예약시각이 도래(<=now)한 RESERVED 행 → PENDING 으로 승격 후 결과 폴링.
    List<AdminSmsEntity> findBySendStatusAndReserveTimeLessThanEqual(
            SendStatus sendStatus, LocalDateTime reserveTimeAtOrBefore);

    // 예약 취소 대상 조회 — 같은 reserve_id 로 묶인 전 행.
    List<AdminSmsEntity> findByReserveId(String reserveId);

    // 전역 발송내역 조회(페이지·필터). sentBy=null 이면 전체(관리자), 값이 있으면 해당 발송자만.
    // 단일값 연관(sender)만 fetch join 하므로 Pageable 과 함께 써도 in-memory 페이징 경고가 없다.
    @Query(value = "select a from AdminSmsEntity a "
            + "left join fetch a.sender su "
            + "where (:sentBy is null or a.sentBy = :sentBy) "
            + "and (:sendStatus is null or a.sendStatus = :sendStatus) "
            + "and (:dateFrom is null or coalesce(a.sentAt, a.reserveTime) >= :dateFrom) "
            + "and (:dateTo is null or coalesce(a.sentAt, a.reserveTime) < :dateTo) "
            + "and (:keyword is null or a.toPhone like concat('%', :keyword, '%') "
            + "     or lower(a.recipientLabel) like lower(concat('%', :keyword, '%'))) "
            // 예약건(sent_at=null)도 reserve_time 기준으로 정렬 대열에 포함 — 최신순.
            + "order by coalesce(a.sentAt, a.reserveTime) desc",
            countQuery = "select count(a) from AdminSmsEntity a "
                    + "where (:sentBy is null or a.sentBy = :sentBy) "
                    + "and (:sendStatus is null or a.sendStatus = :sendStatus) "
                    + "and (:dateFrom is null or coalesce(a.sentAt, a.reserveTime) >= :dateFrom) "
                    + "and (:dateTo is null or coalesce(a.sentAt, a.reserveTime) < :dateTo) "
                    + "and (:keyword is null or a.toPhone like concat('%', :keyword, '%') "
                    + "     or lower(a.recipientLabel) like lower(concat('%', :keyword, '%')))")
    Page<AdminSmsEntity> findPageByFilters(
            @Param("sentBy") Long sentBy,
            @Param("sendStatus") SendStatus sendStatus,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("keyword") String keyword,
            Pageable pageable);
}
