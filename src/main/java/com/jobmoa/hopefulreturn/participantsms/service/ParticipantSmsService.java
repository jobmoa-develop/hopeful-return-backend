package com.jobmoa.hopefulreturn.participantsms.service;

import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsListResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsPageResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import java.time.LocalDate;

public interface ParticipantSmsService {

    SendSmsResponse send(Long userId, SendSmsRequest request);

    ParticipantSmsListResponse findByCourseParticipant(Long courseParticipantId);

    ParticipantSmsDetailResponse findById(Long smsId);

    // PENDING 발송 이력의 실제 전달 결과를 SENS 발송결과 조회로 갱신(스케줄러가 주기 호출). 갱신 건수 반환.
    int pollPendingResults();

    // 예약시각이 도래한 RESERVED 건을 PENDING 으로 승격(스케줄러가 주기 호출). 이후 pollPendingResults 가 전달결과를 반영. 승격 건수 반환.
    int promoteDueReservations();

    // 예약 취소 사전 확인 — reserveId 로 함께 취소될(예약중) 대상 인원·수신자명을 반환.
    ReservationCancelPreviewResponse previewReservationCancel(String reserveId);

    // 예약 취소 실행 — SENS 예약취소 후 해당 reserveId 의 RESERVED 건을 CANCELED 로 전이. 취소 건수 반환.
    int cancelReservation(String reserveId);

    // 특정 발송 이력을 SENS 로 즉시 재조회해 상태·messageId·결과를 갱신하고 상세를 반환(수동 재조회).
    ParticipantSmsDetailResponse refreshResult(Long smsId);

    // 전역 발송내역 조회(페이지·필터). effectiveSentBy=null 이면 전체, 값이 있으면 해당 발송자만.
    ParticipantSmsPageResponse findSmsHistoryPage(
            Long effectiveSentBy,
            String sendStatus,
            Integer courseNumber,
            Integer localCourseNumber,
            Long regionId,
            Long parentRegionId,
            LocalDate sentDateFrom,
            LocalDate sentDateTo,
            String keyword,
            Integer page,
            Integer size);
}
