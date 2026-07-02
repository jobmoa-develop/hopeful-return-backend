package com.jobmoa.hopefulreturn.attendanceleave.service;

import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveRequestDto;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponseDto;
import java.util.List;

public interface AttendanceLeaveService {

    AttendanceLeaveResponseDto create(AttendanceLeaveRequestDto requestDto);

    AttendanceLeaveResponseDto findById(Long id);

    List<AttendanceLeaveResponseDto> findAll();

    AttendanceLeaveResponseDto update(Long id, AttendanceLeaveRequestDto requestDto);

    void delete(Long id);
}
