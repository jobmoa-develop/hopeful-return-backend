package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;

public interface ParticipantService {

    ParticipantCreatedResponse create(CreateParticipantRequest request);

    ParticipantListResponse findAll(Integer page, Integer size, String name, String phone);

    CheckPhoneResponse checkPhone(String phone);

    ParticipantResponse findById(Long participantId);

    ParticipantUpdatedResponse update(Long participantId, UpdateParticipantRequest request);

    ParticipantDeletedResponse delete(Long participantId);
}
