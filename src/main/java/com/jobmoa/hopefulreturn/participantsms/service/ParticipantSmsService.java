package com.jobmoa.hopefulreturn.participantsms.service;

import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsRequestDto;
import com.jobmoa.hopefulreturn.participantsms.model.dto.ParticipantSmsResponseDto;
import java.util.List;

public interface ParticipantSmsService {

    ParticipantSmsResponseDto create(ParticipantSmsRequestDto requestDto);

    ParticipantSmsResponseDto findById(Long id);

    List<ParticipantSmsResponseDto> findAll();

    ParticipantSmsResponseDto update(Long id, ParticipantSmsRequestDto requestDto);

    void delete(Long id);
}
