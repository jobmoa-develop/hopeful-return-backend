package com.jobmoa.hopefulreturn.adminsms.service;

import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminReservationCancelPreviewResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsRequest;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSendSmsResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsDetailResponse;
import com.jobmoa.hopefulreturn.adminsms.model.dto.AdminSmsPageResponse;
import java.time.LocalDate;

/**
 * 관리자 임의 번호 문자 발송·이력·예약. 참여자 SMS(ParticipantSmsService)와 동등 기능이나
 * course_participant 결합이 없어 수신번호(to_phone)를 직접 다룬다.
 */
public interface AdminSmsService {

    AdminSendSmsResponse send(Long userId, AdminSendSmsRequest request);

    // 발송결과 폴링: PENDING 행의 실제 전달상태를 SENS 결과조회로 갱신. 갱신 건수 반환.
    int pollPendingResults();

    // 예약 승격: 예약시각 도래한 RESERVED → PENDING. 승격 건수 반환.
    int promoteDueReservations();

    AdminReservationCancelPreviewResponse previewReservationCancel(String reserveId);

    int cancelReservation(String reserveId);

    AdminSmsDetailResponse refreshResult(Long adminSmsId);

    AdminSmsDetailResponse findById(Long adminSmsId);

    AdminSmsPageResponse findSmsHistoryPage(
            Long effectiveSentBy,
            String sendStatus,
            LocalDate sentDateFrom,
            LocalDate sentDateTo,
            String keyword,
            Integer page,
            Integer size);
}
