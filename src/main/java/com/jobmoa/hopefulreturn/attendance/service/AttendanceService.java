package com.jobmoa.hopefulreturn.attendance.service;

import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDeletedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceDetailResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceListResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.AttendanceUpdatedResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.BulkAttendanceResponse;
import com.jobmoa.hopefulreturn.attendance.model.dto.RegisterAttendanceRequest;
import com.jobmoa.hopefulreturn.attendance.model.dto.UpdateAttendanceRequest;

public interface AttendanceService {

    AttendanceResponse register(RegisterAttendanceRequest request);

    BulkAttendanceResponse registerBulk(BulkAttendanceRequest request);

    AttendanceListResponse findAll(Long courseId, Integer dayNo, String status, Integer page, Integer size);

    AttendanceDetailResponse findById(Long attendanceId);

    AttendanceUpdatedResponse update(Long attendanceId, UpdateAttendanceRequest request);

    AttendanceDeletedResponse delete(Long attendanceId);
}
