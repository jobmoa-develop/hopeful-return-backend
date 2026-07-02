package com.jobmoa.hopefulreturn.participantsms.service;

import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsRequestDto;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ParticipantSmsServiceImpl implements ParticipantSmsService {

    @Override
    public ParticipantSmsResponseDto create(ParticipantSmsRequestDto requestDto) {
        return null;
    }

    @Override
    public ParticipantSmsResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<ParticipantSmsResponseDto> findAll() {
        return null;
    }

    @Override
    public ParticipantSmsResponseDto update(Long id, ParticipantSmsRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
