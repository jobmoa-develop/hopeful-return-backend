package com.jobmoa.hopefulreturn.course.service;

import com.jobmoa.hopefulreturn.course.model.dto.CourseRequestDto;
import com.jobmoa.hopefulreturn.course.model.dto.CourseResponseDto;
import java.util.List;

public interface CourseService {

    CourseResponseDto create(CourseRequestDto requestDto);

    CourseResponseDto findById(Long id);

    List<CourseResponseDto> findAll();

    CourseResponseDto update(Long id, CourseRequestDto requestDto);

    void delete(Long id);
}
