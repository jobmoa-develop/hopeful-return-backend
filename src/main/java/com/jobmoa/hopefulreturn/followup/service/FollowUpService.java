package com.jobmoa.hopefulreturn.followup.service;

import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.CreateFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.DeleteFollowUpResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpDetailResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpListResponse;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpRequest;
import com.jobmoa.hopefulreturn.followup.model.dto.UpdateFollowUpResponse;

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
}
