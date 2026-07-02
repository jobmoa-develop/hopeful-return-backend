package com.jobmoa.hopefulreturn.coursestaff.service;

import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffRequestDto;
import com.jobmoa.hopefulreturn.coursestaff.model.dto.CourseStaffResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CourseStaffServiceImpl implements CourseStaffService {

    @Override
    public CourseStaffResponseDto create(CourseStaffRequestDto requestDto) {
        return null;
    }

    @Override
    public CourseStaffResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<CourseStaffResponseDto> findAll() {
        return null;
    }

    @Override
    public CourseStaffResponseDto update(Long id, CourseStaffRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
