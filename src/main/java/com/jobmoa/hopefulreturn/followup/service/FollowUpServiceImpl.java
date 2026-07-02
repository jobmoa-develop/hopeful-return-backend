package com.jobmoa.hopefulreturn.followup.service;

import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpRequestDto;
import com.jobmoa.hopefulreturn.followup.model.dto.FollowUpResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FollowUpServiceImpl implements FollowUpService {

    @Override
    public FollowUpResponseDto create(FollowUpRequestDto requestDto) {
        return null;
    }

    @Override
    public FollowUpResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<FollowUpResponseDto> findAll() {
        return null;
    }

    @Override
    public FollowUpResponseDto update(Long id, FollowUpRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
