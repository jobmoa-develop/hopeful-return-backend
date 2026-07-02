package com.jobmoa.hopefulreturn.attendanceleave.service;

import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveRequestDto;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponseDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AttendanceLeaveServiceImpl implements AttendanceLeaveService {

    @Override
    public AttendanceLeaveResponseDto create(AttendanceLeaveRequestDto requestDto) {
        return null;
    }

    @Override
    public AttendanceLeaveResponseDto findById(Long id) {
        return null;
    }

    @Override
    public List<AttendanceLeaveResponseDto> findAll() {
        return null;
    }

    @Override
    public AttendanceLeaveResponseDto update(Long id, AttendanceLeaveRequestDto requestDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }
}
