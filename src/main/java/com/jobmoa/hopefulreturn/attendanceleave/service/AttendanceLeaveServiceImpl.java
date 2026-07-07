package com.jobmoa.hopefulreturn.attendanceleave.service;

import com.jobmoa.hopefulreturn.attendance.entity.AttendanceEntity;
import com.jobmoa.hopefulreturn.attendance.repository.AttendanceRepository;
import com.jobmoa.hopefulreturn.attendanceleave.entity.AttendanceLeaveEntity;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDeletedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveDetailResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.AttendanceLeaveUpdatedResponse;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.RegisterAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.model.dto.UpdateAttendanceLeaveRequest;
import com.jobmoa.hopefulreturn.attendanceleave.repository.AttendanceLeaveRepository;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.entity.CourseParticipantEntity;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceLeaveServiceImpl implements AttendanceLeaveService {

    private final AttendanceLeaveRepository attendanceLeaveRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public AttendanceLeaveResponse register(RegisterAttendanceLeaveRequest request) {
        if (!attendanceRepository.existsById(request.attendanceId())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND);
        }

        AttendanceLeaveEntity entity = AttendanceLeaveEntity.builder()
                .attendanceId(request.attendanceId())
                .leaveTime(request.leaveTime())
                .returnTime(request.returnTime())
                .reason(request.reason())
                .createdAt(LocalDateTime.now())
                .build();

        AttendanceLeaveEntity saved = attendanceLeaveRepository.save(entity);
        return new AttendanceLeaveResponse(
                saved.getAttendanceLeaveId(),
                saved.getAttendanceId(),
                saved.getLeaveTime(),
                saved.getReturnTime(),
                saved.getReason(),
                saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceLeaveDetailResponse findById(Long attendanceLeaveId) {
        AttendanceLeaveEntity entity = findEntity(attendanceLeaveId);
        return new AttendanceLeaveDetailResponse(
                entity.getAttendanceLeaveId(),
                entity.getAttendanceId(),
                participantName(entity),
                entity.getLeaveTime(),
                entity.getReturnTime(),
                entity.getReason());
    }

    @Override
    public AttendanceLeaveUpdatedResponse update(Long attendanceLeaveId, UpdateAttendanceLeaveRequest request) {
        AttendanceLeaveEntity entity = findEntity(attendanceLeaveId);
        if (request.leaveTime() != null) {
            entity.setLeaveTime(request.leaveTime());
        }
        if (request.returnTime() != null) {
            entity.setReturnTime(request.returnTime());
        }
        if (request.reason() != null) {
            entity.setReason(request.reason());
        }
        attendanceLeaveRepository.save(entity);

        // attendance_leave 테이블에 updated_at 컬럼이 없어 응답 시점 기준 값을 반환한다(미영속).
        return new AttendanceLeaveUpdatedResponse(entity.getAttendanceLeaveId(), LocalDateTime.now());
    }

    @Override
    public AttendanceLeaveDeletedResponse delete(Long attendanceLeaveId) {
        AttendanceLeaveEntity entity = findEntity(attendanceLeaveId);
        attendanceLeaveRepository.delete(entity);
        return new AttendanceLeaveDeletedResponse(true);
    }

    private AttendanceLeaveEntity findEntity(Long attendanceLeaveId) {
        return attendanceLeaveRepository.findById(attendanceLeaveId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_LEAVE_NOT_FOUND));
    }

    private String participantName(AttendanceLeaveEntity leave) {
        AttendanceEntity attendance = leave.getAttendance();
        if (attendance == null) {
            return null;
        }
        CourseParticipantEntity cp = attendance.getCourseParticipant();
        if (cp == null || cp.getParticipant() == null) {
            return null;
        }
        return cp.getParticipant().getName();
    }
}
