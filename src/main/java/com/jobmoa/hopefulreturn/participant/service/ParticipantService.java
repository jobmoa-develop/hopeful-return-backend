package com.jobmoa.hopefulreturn.participant.service;

import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantRequestDto;
import com.jobmoa.hopefulreturn.participant.model.dto.ParticipantResponseDto;
import java.util.List;

public interface ParticipantService {

    ParticipantResponseDto create(ParticipantRequestDto requestDto);

    ParticipantResponseDto findById(Long id);

    List<ParticipantResponseDto> findAll();

    ParticipantResponseDto update(Long id, ParticipantRequestDto requestDto);

    void delete(Long id);
}
