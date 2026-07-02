package com.jobmoa.hopefulreturn.participantmemo.service;

import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoRequestDto;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoResponseDto;
import java.util.List;

public interface ParticipantMemoService {

    ParticipantMemoResponseDto create(ParticipantMemoRequestDto requestDto);

    ParticipantMemoResponseDto findById(Long id);

    List<ParticipantMemoResponseDto> findAll();

    ParticipantMemoResponseDto update(Long id, ParticipantMemoRequestDto requestDto);

    void delete(Long id);
}
