package com.jobmoa.hopefulreturn.followupcounsel.service;

import com.jobmoa.hopefulreturn.followupcounsel.model.dto.CreateFollowUpCounselRequest;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselCreatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDeletedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselDetailResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselListResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.FollowUpCounselUpdatedResponse;
import com.jobmoa.hopefulreturn.followupcounsel.model.dto.UpdateFollowUpCounselRequest;

public interface FollowUpCounselService {

    FollowUpCounselCreatedResponse create(CreateFollowUpCounselRequest request);

    FollowUpCounselListResponse findAll(Long courseParticipantId, Long counselorScopeId);

    FollowUpCounselDetailResponse findById(Long followUpCounselId, Long counselorScopeId);

    FollowUpCounselUpdatedResponse update(Long followUpCounselId, UpdateFollowUpCounselRequest request);

    FollowUpCounselDeletedResponse delete(Long followUpCounselId);
}
