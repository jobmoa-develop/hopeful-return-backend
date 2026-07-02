package com.jobmoa.hopefulreturn.followup.service;

import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpRequestDto;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpResponseDto;
import java.util.List;

public interface FollowUpService {

    FollowUpResponseDto create(FollowUpRequestDto requestDto);

    FollowUpResponseDto findById(Long id);

    List<FollowUpResponseDto> findAll();

    FollowUpResponseDto update(Long id, FollowUpRequestDto requestDto);

    void delete(Long id);
}
