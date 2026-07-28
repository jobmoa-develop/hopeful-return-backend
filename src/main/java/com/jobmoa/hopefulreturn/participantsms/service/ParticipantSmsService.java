package com.jobmoa.hopefulreturn.participantsms.service;

import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsListResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsPageResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;
import java.time.LocalDate;

public interface ParticipantSmsService {

    SendSmsResponse send(Long userId, SendSmsRequest request);

    ParticipantSmsListResponse findByCourseParticipant(Long courseParticipantId);

    ParticipantSmsDetailResponse findById(Long smsId);

    // PENDING 발송 이력의 실제 전달 결과를 SENS 발송결과 조회로 갱신(스케줄러가 주기 호출). 갱신 건수 반환.
    int pollPendingResults();

    // 특정 발송 이력을 SENS 로 즉시 재조회해 상태·messageId·결과를 갱신하고 상세를 반환(수동 재조회).
    ParticipantSmsDetailResponse refreshResult(Long smsId);

    // 전역 발송내역 조회(페이지·필터). effectiveSentBy=null 이면 전체, 값이 있으면 해당 발송자만.
    ParticipantSmsPageResponse findSmsHistoryPage(
            Long effectiveSentBy,
            String sendStatus,
            Integer courseNumber,
            Long regionId,
            LocalDate sentDateFrom,
            LocalDate sentDateTo,
            String keyword,
            Integer page,
            Integer size);
}
