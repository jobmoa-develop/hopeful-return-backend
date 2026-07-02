package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantRequestDto;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CourseParticipantServiceImpl implements CourseParticipantService {

    @Override
    public CourseParticipantResponseDto create(CourseParticipantRequestDto requestDto) {
        return null;
    }

    @Override
    public CourseParticipantResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<CourseParticipantResponseDto> findAll() {
        return null;
    }

    @Override
    public CourseParticipantResponseDto update(Long id, CourseParticipantRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
