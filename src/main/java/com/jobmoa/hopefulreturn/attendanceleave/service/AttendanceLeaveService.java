package com.jobmoa.hopefulreturn.attendanceleave.service;

import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDeletedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDetailResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveUpdatedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.RegisterAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.UpdateAttendanceLeaveRequest;

public interface AttendanceLeaveService {

    AttendanceLeaveResponse register(RegisterAttendanceLeaveRequest request);

    AttendanceLeaveDetailResponse findById(Long attendanceLeaveId);

    AttendanceLeaveUpdatedResponse update(Long attendanceLeaveId, UpdateAttendanceLeaveRequest request);

    AttendanceLeaveDeletedResponse delete(Long attendanceLeaveId);
}
