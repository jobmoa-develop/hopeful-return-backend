package com.jobmoa.hopefulreturn.followup.service;

import com.jobmoa.hopefulreturn.followup.model.dto.*;

public interface FollowUpService {

    CreateFollowUpResponse create(CreateFollowUpRequest request);

    FollowUpListResponse findAll(
            String name,
            Long regionId,
            Integer courseNumber,
            Long counselorScopeId,
            Integer page,
            Integer size);

    FollowUpDetailResponse findById(Long followUpId, Long counselorScopeId);

    UpdateFollowUpResponse update(Long followUpId, UpdateFollowUpRequest request);

    DeleteFollowUpResponse delete(Long followUpId);

    FollowUpStatsResponse getStats(Long regionId, Integer courseNumber, Long counselorScopeId);
}
