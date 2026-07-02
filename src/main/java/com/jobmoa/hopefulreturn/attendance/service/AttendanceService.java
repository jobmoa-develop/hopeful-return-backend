package com.jobmoa.hopefulreturn.attendance.service;

import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceRequestDto;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponseDto;
import java.util.List;

public interface AttendanceService {

    AttendanceResponseDto create(AttendanceRequestDto requestDto);

    AttendanceResponseDto findById(Long id);

    List<AttendanceResponseDto> findAll();

    AttendanceResponseDto update(Long id, AttendanceRequestDto requestDto);

    void delete(Long id);
}
