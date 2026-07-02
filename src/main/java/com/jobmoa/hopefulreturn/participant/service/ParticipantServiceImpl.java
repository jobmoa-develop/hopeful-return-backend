package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantRequestDto;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ParticipantServiceImpl implements ParticipantService {

    @Override
    public ParticipantResponseDto create(ParticipantRequestDto requestDto) {
        return null;
    }

    @Override
    public ParticipantResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<ParticipantResponseDto> findAll() {
        return null;
    }

    @Override
    public ParticipantResponseDto update(Long id, ParticipantRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
