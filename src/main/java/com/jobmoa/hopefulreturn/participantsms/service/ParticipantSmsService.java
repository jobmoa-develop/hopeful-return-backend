package com.jobmoa.hopefulreturn.participantsms.service;

import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsDetailResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsListResponse;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsRequest;
import com.jobmoa.hopefulreturn.participantsms.model.dto.SendSmsResponse;

public interface ParticipantSmsService {

    SendSmsResponse send(Long userId, SendSmsRequest request);

    ParticipantSmsListResponse findByCourseParticipant(Long courseParticipantId);

    ParticipantSmsDetailResponse findById(Long smsId);
}
