package com.jobmoa.hopefulreturn.coursestaff.service;

import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffRequestDto;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffResponseDto;
import java.util.List;

public interface CourseStaffService {

    CourseStaffResponseDto create(CourseStaffRequestDto requestDto);

    CourseStaffResponseDto findById(Long id);

    List<CourseStaffResponseDto> findAll();

    CourseStaffResponseDto update(Long id, CourseStaffRequestDto requestDto);

    void delete(Long id);
}
