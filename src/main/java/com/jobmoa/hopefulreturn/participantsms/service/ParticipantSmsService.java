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
