package com.jobmoa.hopefulreturn.attendance.service;

import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceRequestDto;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Override
    public AttendanceResponseDto create(AttendanceRequestDto requestDto) {
        return null;
    }

    @Override
    public AttendanceResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<AttendanceResponseDto> findAll() {
        return null;
    }

    @Override
    public AttendanceResponseDto update(Long id, AttendanceRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
