package com.jobmoa.hopefulreturn.participantmemo.service;

import com.jobmoa.hopefulreturn.participantmemo.model.dto.CreateParticipantMemoRequest;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoCreatedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoDeletedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoDetailResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoListResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoUpdatedResponse;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.UpdateParticipantMemoRequest;

public interface ParticipantMemoService {

    ParticipantMemoCreatedResponse create(Long userId, CreateParticipantMemoRequest request);

    ParticipantMemoListResponse findAll(Long courseParticipantId);

    ParticipantMemoDetailResponse findById(Long memoId);

    ParticipantMemoUpdatedResponse update(Long memoId, UpdateParticipantMemoRequest request);

    ParticipantMemoDeletedResponse delete(Long memoId);
}
