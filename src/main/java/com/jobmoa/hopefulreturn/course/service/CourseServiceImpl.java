package com.jobmoa.hopefulreturn.course.service;

import com.jobmoa.hopefulreturn.course.model.dto.CourseRequestDto;
import com.jobmoa.hopefulreturn.course.model.dto.CourseResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl implements CourseService {

    @Override
    public CourseResponseDto create(CourseRequestDto requestDto) {
        return null;
    }

    @Override
    public CourseResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<CourseResponseDto> findAll() {
        return null;
    }

    @Override
    public CourseResponseDto update(Long id, CourseRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
