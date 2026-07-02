package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantRequestDto;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.CourseParticipantResponseDto;
import java.util.List;

public interface CourseParticipantService {

    CourseParticipantResponseDto create(CourseParticipantRequestDto requestDto);

    CourseParticipantResponseDto findById(Long id);

    List<CourseParticipantResponseDto> findAll();

    CourseParticipantResponseDto update(Long id, CourseParticipantRequestDto requestDto);

    void delete(Long id);
}
