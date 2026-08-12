package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.participant.model.dto.CheckPhoneResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.CreateParticipantRequest;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantCreatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantDeletedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantListResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantUpdatedResponse;
import com.jobmoa.hopefulreturn.participant.model.dto.UpdateParticipantRequest;
import java.time.LocalDate;
import java.util.Set;

public interface ParticipantService {

    ParticipantCreatedResponse create(CreateParticipantRequest request);

    ParticipantListResponse findAll(
            Integer page, Integer size, String name, String phone, Long regionId, Long parentRegionId,
            Integer courseNumber, Integer localCourseNumber, Set<Long> allowedParticipantIds,
            LocalDate registerDateFrom, LocalDate registerDateTo, String sortBy, String sortOrder);

    CheckPhoneResponse checkPhone(String phone);

    ParticipantResponse findById(Long participantId, Set<Long> allowedParticipantIds);

    ParticipantUpdatedResponse update(Long participantId, UpdateParticipantRequest request);

    ParticipantDeletedResponse delete(Long participantId);
}
