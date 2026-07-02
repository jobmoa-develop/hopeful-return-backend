package com.jobmoa.hopefulreturn.participantmemo.service;

import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoRequestDto;
import com.jobmoa.hopefulreturn.participantmemo.model.dto.ParticipantMemoResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ParticipantMemoServiceImpl implements ParticipantMemoService {

    @Override
    public ParticipantMemoResponseDto create(ParticipantMemoRequestDto requestDto) {
        return null;
    }

    @Override
    public ParticipantMemoResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<ParticipantMemoResponseDto> findAll() {
        return null;
    }

    @Override
    public ParticipantMemoResponseDto update(Long id, ParticipantMemoRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
